package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RimiText2Receipt extends BasicText2Receipt<RimiContext> {
    @Override
    protected RimiContext getContext(String text) {
        return new RimiContext(text);
    }

    @Override
    protected String getShopBrand(RimiContext context) {
        return "Rimi";
    }

    @Override
    protected String getShopName(RimiContext context) {
        return context.getLineContaining("Rīga", 1);
    }

    @Override
    protected String getCashRegisterNumber(RimiContext context) {
        return context.getLineContaining("Kase Nr", 0);
    }

    @Override
    protected BigDecimal getTotalSavings(RimiContext context) {
        String text = "Nopelnītā Mans Rimi nauda";
        String line = context.getLineContaining(text, 0);
        if (line != null) {
            return ConversionUtils.getBigDecimalAfterToken(line, text);
        } else {
            return null;
        }
    }

    @Override
    protected BigDecimal getTotalPayment(RimiContext context) {
        Pattern paymentAmountPattern = Pattern.compile("Samaksai EUR +(.*)");
        String paymentAmount = getFirstGroup(context, paymentAmountPattern);
        Pattern totalAmountPattern = Pattern.compile("KOPA: +(\\d+[.,]\\d+) +EUR.*");
        String totalAmount = getFirstGroup(context, totalAmountPattern);
        Pattern bankCardPattern = Pattern.compile("Bankas karte +(\\d+[.,]\\d+).*");
        String bankCardAmount = getFirstGroup(context, bankCardPattern);
        return ConversionUtils.getBigDecimal(paymentAmount, totalAmount, bankCardAmount);
    }

    private static String getFirstGroup(RimiContext context, Pattern pattern) {
        return ConversionUtils.getFirstGroup(context.getLineMatching(pattern, 0), pattern);
    }

    @Override
    protected BigDecimal getTotalVat(RimiContext context) {
        String text = "Nodoklis Ar PVN Bez PVN PVN summa";
        String line = context.getLinesAfterContaining(text).get(0);
        return ConversionUtils.getBigDecimal(line.split(" ")[5]);
    }

    @Override
    protected BigDecimal getShopBrandMoneyAccumulated(RimiContext context) {
        String text = "Mans Rimi naudas uzkrājums";
        String line = context.getLineContaining(text, 0);
        return ConversionUtils.getBigDecimalAfterToken(line, text);
    }

    @Override
    protected String getDocumentNumber(RimiContext context) {
        String text = "Dok. Nr.:";
        String line = context.getLineContaining(text, 0);
        return line.split(text)[0].trim();
    }

    @Override
    protected LocalDateTime getReceiptDateTime(RimiContext context) {
        return LocalDateTime.parse(
                context.getLine(-3),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }

    @Override
    protected Collection<? extends ReceiptItem> getItems(RimiContext context) {
        Pattern countPricePerUnitAndSumPattern = Pattern.compile("(\\d+([.,]\\d+)?) (\\w+) X (\\d+([.,]\\d+)?) (\\w+|\\w+\\/\\w+) (\\d+([.,]\\d+)?)( \\w)?");
        Pattern discountLinePattern = Pattern.compile("(Atī\\.|Atl\\.)\\s+-(\\d+[.,]\\d+)\\s+Gala\\s+cena\\s+(\\d+[.,]\\d+).*");
        List<ReceiptItem> items = new ArrayList<>();
        List<String> linesBetween = context.getLinesBetween("KLIENTS:", "Maksājumu karte");
        linesBetween.add("HackLineThatDoesNotMatchAnyPatternButLetsUsProcessLastItemInList");
        List<String> itemNameBuilder = new ArrayList<>();
        Matcher priceLineMatcher = null;
        Matcher discountLineMatcher = null;
        for (String line : linesBetween) {

            if (line.isBlank()) {
                continue;
            }

            Matcher itemNumbersMatcher = countPricePerUnitAndSumPattern.matcher(line);
            if (itemNumbersMatcher.matches()) {
                priceLineMatcher = itemNumbersMatcher;
                continue;
            }

            if (priceLineMatcher == null) {
                itemNameBuilder.add(line);
            } else {
                Matcher discountLinePatternMatcher = discountLinePattern.matcher(line);
                if (discountLinePatternMatcher.matches()) {
                    discountLineMatcher = discountLinePatternMatcher;
                } else {
                    BigDecimal finalCost;
                    BigDecimal discount = BigDecimal.ZERO;
                    if (discountLineMatcher != null) {
                        finalCost = ConversionUtils.getBigDecimal(discountLineMatcher.group(3));
                        discount = ConversionUtils.getBigDecimal(discountLineMatcher.group(2));
                    } else {
                        finalCost = ConversionUtils.getBigDecimal(priceLineMatcher.group(7));
                    }

                    ReceiptItem item = ReceiptItem
                            .builder()
                            .description(Strings.join(itemNameBuilder, ' ').trim())
                            .count(ConversionUtils.getBigDecimal(priceLineMatcher.group(1)))
                            .units(priceLineMatcher.group(3))
                            .pricePerUnit(ConversionUtils.getBigDecimal(priceLineMatcher.group(4)))
                            .discount(discount)
                            .finalCost(finalCost)
                            .build();

                    items.add(item);
                    itemNameBuilder.clear();
                    itemNameBuilder.add(line);
                    priceLineMatcher = null;
                    discountLineMatcher = null;
                }
            }
        }
        return items;
    }
}
