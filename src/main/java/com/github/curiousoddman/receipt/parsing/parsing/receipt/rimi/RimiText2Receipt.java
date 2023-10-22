package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.model.ReceiptNumber;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTessWord;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import com.github.curiousoddman.receipt.parsing.validation.ItemNumbersValidator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.parsing.Patterns.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RimiText2Receipt extends BasicText2Receipt<RimiContext> {
    private final MyTesseract          tesseract;
    private final ItemNumbersValidator itemNumbersValidator;

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
    protected ReceiptNumber getTotalSavings(RimiContext context) {
        String totalSavings = getFirstGroup(context, SAVINGS_AMOUNT);
        if (totalSavings != null) {
            return ConversionUtils.getReceiptNumber(totalSavings);
        } else {
            return new ReceiptNumber(BigDecimal.ZERO, null);
        }
    }

    @Override
    protected ReceiptNumber getTotalPayment(RimiContext context) {
        String paymentAmount = getFirstGroup(context, PAYMENT_SUM);
        String totalAmount = getFirstGroup(context, TOTAL_AMOUNT);
        String bankCardAmount = getFirstGroup(context, BANK_CARD_AMOUNT);
        return ConversionUtils.getReceiptNumber(paymentAmount, totalAmount, bankCardAmount);
    }

    private static String getFirstGroup(RimiContext context, Pattern pattern) {
        return ConversionUtils.getFirstGroup(context.getLineMatching(pattern, 0), pattern);
    }

    @Override
    protected ReceiptNumber getTotalVat(RimiContext context) {
        String line = context.getNextLinesAfterMatching(LINE_BEFORE_VAT_AMOUNTS_LINE).get(0);
        String word = line.split(" ")[5];
        try {
            return ConversionUtils.getReceiptNumber(word);
        } catch (Exception e) {
            log.info("Failed to parse vat - retrying... {}", e.getMessage());
            MyTessWord myTessWord = context.getTessWord(word);
            try {
                String text = tesseract.doOCR(context.getOriginalFile(), myTessWord.getWordRect());
                return ConversionUtils.getReceiptNumber(text);
            } catch (Exception e1) {
                log.error("", e1);
                return new ReceiptNumber(BigDecimal.ZERO, null);
            }
        }
    }

    @Override
    protected ReceiptNumber getShopBrandMoneyAccumulated(RimiContext context) {
        String text = "Nopelnītā Mans Rimi nauda";
        String line = context.getLineContaining(text, 0);
        if (line != null) {
            return ConversionUtils.getBigDecimalAfterToken(line, text);
        } else {
            return new ReceiptNumber(BigDecimal.ZERO, null);
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
                    ReceiptItem item = createItem(line, discountLineMatcher, priceLineMatcher, itemNameBuilder);
                    if (!itemNumbersValidator.isItemValid(item)) {
                        item = tryOcrNumbersAgain(context, item);
                    }
                    items.add(item);
                    priceLineMatcher = null;
                    discountLineMatcher = null;
                }
            }
        }
        return items;
    }

    private ReceiptItem createItem(String line, Matcher discountLineMatcher, Matcher priceLineMatcher, List<String> itemNameBuilder) {
        ReceiptNumber finalCost;
        ReceiptNumber discount = new ReceiptNumber(BigDecimal.ZERO, null);
        if (discountLineMatcher != null) {
            finalCost = ConversionUtils.getReceiptNumber(discountLineMatcher.group(2));
            discount = ConversionUtils.getReceiptNumber(discountLineMatcher.group(1));
        } else {
            String finalCostGroupValue = priceLineMatcher.group(7).trim();
            // If line looks like this: 2 gab X 2,99 EUR 5,98 A -> then group 7 is 5,98 A
            int indexOfSpace = finalCostGroupValue.indexOf(' ');
            if (indexOfSpace >= 0) {
                finalCost = ConversionUtils.getReceiptNumber(finalCostGroupValue.substring(0, indexOfSpace));
            } else {
                finalCost = ConversionUtils.getReceiptNumber(finalCostGroupValue);
            }
        }

        String countText = priceLineMatcher.group(1);
        String pricePerUnitText = priceLineMatcher.group(4);
        ReceiptItem item = ReceiptItem
                .builder()
                .description(Strings.join(itemNameBuilder, ' ').trim())
                .count(ConversionUtils.getReceiptNumber(countText))
                .units(priceLineMatcher.group(3))
                .pricePerUnit(ConversionUtils.getReceiptNumber(pricePerUnitText))
                .discount(discount)
                .finalCost(finalCost)
                .build();

        itemNameBuilder.clear();
        itemNameBuilder.add(line);
        return item;
    }

    @SneakyThrows
    private ReceiptItem tryOcrNumbersAgain(RimiContext context, ReceiptItem item) {
        List<ReceiptNumberWithSetter> allNumbers = List.of(
                new ReceiptNumberWithSetter(item.getDiscount(), ReceiptItem::setDiscount),
                new ReceiptNumberWithSetter(item.getFinalCost(), ReceiptItem::setFinalCost),
                new ReceiptNumberWithSetter(item.getPricePerUnit(), ReceiptItem::setPricePerUnit),
                new ReceiptNumberWithSetter(item.getCount(), ReceiptItem::setCount)
        );
        for (ReceiptNumberWithSetter rnWithSetter : allNumbers) {
            ReceiptNumber rn = rnWithSetter.rn();
            BiConsumer<ReceiptItem, ReceiptNumber> setter = rnWithSetter.setter();
            List<MyTessWord> tessWords = context.getTessWords(rn.text());
            if (tessWords.size() == 1) {
                MyTessWord myTessWord = tessWords.get(0);
                String newValue = tesseract.doOCR(context.getOriginalFile(), myTessWord.getWordRect());
                ReceiptItem itemCopy = item.toBuilder().build();
                setter.accept(itemCopy, ConversionUtils.getReceiptNumber(newValue));
                if (itemNumbersValidator.isItemValid(itemCopy)) {
                    return itemCopy;
                }
            } else {
                log.error("Cannot find tess word: {}", tessWords);
            }
        }

        log.info("Unable to fix the item");
        return item;
    }

    private record ReceiptNumberWithSetter(ReceiptNumber rn, BiConsumer<ReceiptItem, ReceiptNumber> setter) {

    }
}
