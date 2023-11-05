package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.parseDateTime;
import static com.github.curiousoddman.receipt.parsing.utils.Patterns.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RimiText2Receipt extends BasicText2Receipt<RimiContext> {
    public static final MyBigDecimal         RECEIPT_NUMBER_ZERO = new MyBigDecimal(BigDecimal.ZERO, null);
    private final       MyTesseract          tesseract;
    private final       ItemNumbersValidator itemNumbersValidator;

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
    protected MyBigDecimal getTotalSavings(RimiContext context) {
        String totalSavings = getFirstGroup(context, SAVINGS_AMOUNT);
        if (totalSavings != null) {
            return getReceiptNumber(context, totalSavings);
        } else {
            return RECEIPT_NUMBER_ZERO;
        }
    }

    @Override
    protected MyBigDecimal getTotalPayment(RimiContext context) {
        String paymentAmount = getFirstGroup(context, PAYMENT_SUM);
        String totalAmount = getFirstGroup(context, TOTAL_AMOUNT);
        String bankCardAmount = getFirstGroup(context, BANK_CARD_AMOUNT);
        return ConversionUtils.getReceiptNumber(paymentAmount, totalAmount, bankCardAmount);
    }

    private static String getFirstGroup(RimiContext context, Pattern pattern) {
        return ConversionUtils.getFirstGroup(context.getLineMatching(pattern, 0), pattern);
    }

//    @Override
//    protected MyBigDecimal getTotalVat(RimiContext context) {
//        String line = context.getNextLinesAfterMatching(LINE_BEFORE_VAT_AMOUNTS_LINE).get(0);
//        String word = line.split(" ")[5];
//        try {
//            return getReceiptNumber(word);
//        } catch (Exception e) {
//            log.info("Failed to parse vat - retrying... {}", e.getMessage());
//            MyTessWord myTessWord = context.getTessWord(word);
//            try {
//                String text = tesseract.doOCR(context.getOriginalFile(), myTessWord.getWordRect());
//                return getReceiptNumber(text);
//            } catch (Exception e1) {
//                log.error("", e1);
//                return RECEIPT_NUMBER_ZERO;
//            }
//        }
//    }

    @Override
    protected MyBigDecimal getShopBrandMoneyAccumulated(RimiContext context) {
        String text = "Nopelnītā Mans Rimi nauda";
        String line = context.getLineContaining(text, 0);
        if (line != null) {
            return ConversionUtils.getBigDecimalAfterToken(line, text);
        } else {
            return RECEIPT_NUMBER_ZERO;
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
        return parseDateTime(time);
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
                    ReceiptItem item = createItem(context, line, discountLineMatcher, priceLineMatcher, itemNameBuilder);
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

    private ReceiptItem createItem(RimiContext context, String line, Matcher discountLineMatcher, Matcher priceLineMatcher, List<String> itemNameBuilder) {
        MyBigDecimal finalCost;
        MyBigDecimal discount = RECEIPT_NUMBER_ZERO;
        if (discountLineMatcher != null) {
            finalCost = getReceiptNumber(context, discountLineMatcher.group(2));
            discount = getReceiptNumber(context, discountLineMatcher.group(1));
        } else {
            String finalCostGroupValue = priceLineMatcher.group(7).trim();
            // If line looks like this: 2 gab X 2,99 EUR 5,98 A -> then group 7 is 5,98 A
            int indexOfSpace = finalCostGroupValue.indexOf(' ');
            if (indexOfSpace >= 0) {
                finalCost = getReceiptNumber(context, finalCostGroupValue.substring(0, indexOfSpace));
            } else {
                finalCost = getReceiptNumber(context, finalCostGroupValue);
            }
        }

        String countText = priceLineMatcher.group(1);
        String pricePerUnitText = priceLineMatcher.group(4);
        ReceiptItem item = ReceiptItem
                .builder()
                .description(Strings.join(itemNameBuilder, ' ').trim())
                .count(getReceiptNumber(context, countText))
                .units(priceLineMatcher.group(3))
                .pricePerUnit(getReceiptNumber(context, pricePerUnitText))
                .discount(discount)
                .finalCost(finalCost)
                .build();

        itemNameBuilder.clear();
        itemNameBuilder.add(line);
        return item;
    }

    @SneakyThrows
    private MyBigDecimal getReceiptNumber(RimiContext context, String value) {
        try {
            return ConversionUtils.getReceiptNumber(value);
        } catch (Exception e) {
            List<MyTessWord> tessWords = context.getTessWords(value);
            if (tessWords.size() == 1) {
                MyTessWord myTessWord = tessWords.get(0);
                return ConversionUtils.getReceiptNumber(tesseract.doOCR(context.getOriginalFile(), myTessWord.getWordRect()));
            } else {
                log.error("Cannot find tess word: {}", tessWords);
            }
            throw e;
        }
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
            MyBigDecimal rn = rnWithSetter.rn();
            if (rn == RECEIPT_NUMBER_ZERO) {
                continue;
            }
            BiConsumer<ReceiptItem, MyBigDecimal> setter = rnWithSetter.setter();
            List<MyTessWord> tessWords = context.getTessWords(rn.text());
            if (tessWords.size() == 1) {
                MyTessWord myTessWord = tessWords.get(0);
                String newValue = tesseract.doOCR(context.getOriginalFile(), myTessWord.getWordRect());
                ReceiptItem itemCopy = item.toBuilder().build();
                setter.accept(itemCopy, getReceiptNumber(context, newValue));
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

    private record ReceiptNumberWithSetter(MyBigDecimal rn, BiConsumer<ReceiptItem, MyBigDecimal> setter) {

    }
}
