package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTessWord;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.parsing.Patterns.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RimiText2Receipt extends BasicText2Receipt<RimiContext> {
    private final MyTesseract tesseract;

    @Override
    protected RimiContext getContext(MyTessResult tessResult) {
        return new RimiContext(tessResult.inputFile(), tessResult.plainText(), tessResult.tsvText());
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
        String totalSavings = getFirstGroup(context, SAVINGS_AMOUNT);
        if (totalSavings != null) {
            return ConversionUtils.getBigDecimal(totalSavings);
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Override
    protected BigDecimal getTotalPayment(RimiContext context) {
        String paymentAmount = getFirstGroup(context, PAYMENT_SUM);
        String totalAmount = getFirstGroup(context, TOTAL_AMOUNT);
        String bankCardAmount = getFirstGroup(context, BANK_CARD_AMOUNT);
        return ConversionUtils.getBigDecimal(paymentAmount, totalAmount, bankCardAmount);
    }

    private static String getFirstGroup(RimiContext context, Pattern pattern) {
        return ConversionUtils.getFirstGroup(context.getLineMatching(pattern, 0), pattern);
    }

    @Override
    protected BigDecimal getTotalVat(RimiContext context) {
        String line = context.getNextLinesAfterMatching(LINE_BEFORE_VAT_AMOUNTS_LINE).get(0);
        String word = line.split(" ")[5];
        try {
            return ConversionUtils.getBigDecimal(word);
        } catch (Exception e) {
            log.info("Failed to parse vat - retrying... {}", e.getMessage());
            MyTessWord myTessWord = context.getTessWord(word);
            try {
                String text = tesseract.doOCR(context.getOriginalFile(), new Rectangle(myTessWord.left(), myTessWord.top(), myTessWord.width(), myTessWord.height()));
                return ConversionUtils.getBigDecimal(text);
            } catch (Exception e1) {
                log.error("", e1);
                return BigDecimal.ZERO;
            }
        }
    }

    @Override
    protected BigDecimal getShopBrandMoneyAccumulated(RimiContext context) {
        String text = "Nopelnītā Mans Rimi nauda";
        String line = context.getLineContaining(text, 0);
        if (line != null) {
            return ConversionUtils.getBigDecimalAfterToken(line, text);
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Override
    protected String getDocumentNumber(RimiContext context) {
        String text = "Dok. Nr.:";
        String line = context.getLineContaining(text, 0);
        return line.split(text)[0].trim();
    }

    @Override
    protected LocalDateTime getReceiptDateTime(RimiContext context) {
        String time = getFirstGroup(context, RECEIPT_TIME_PATTERN);
        return LocalDateTime.parse(
                time,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );
    }

    @Override
    protected Collection<? extends ReceiptItem> getItems(RimiContext context) {
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

            Matcher itemNumbersMatcher = COUNT_PRICE_AND_SUM_LINE.matcher(line);
            if (itemNumbersMatcher.matches()) {
                priceLineMatcher = itemNumbersMatcher;
                continue;
            }

            if (priceLineMatcher == null) {
                itemNameBuilder.add(line);
            } else {
                Matcher discountLinePatternMatcher = ITEM_DISCOUNT_LINE_PATTERN.matcher(line);
                if (discountLinePatternMatcher.matches()) {
                    discountLineMatcher = discountLinePatternMatcher;
                } else {
                    BigDecimal finalCost;
                    BigDecimal discount = BigDecimal.ZERO;
                    if (discountLineMatcher != null) {
                        finalCost = ConversionUtils.getBigDecimal(discountLineMatcher.group(2));
                        discount = ConversionUtils.getBigDecimal(discountLineMatcher.group(1));
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
