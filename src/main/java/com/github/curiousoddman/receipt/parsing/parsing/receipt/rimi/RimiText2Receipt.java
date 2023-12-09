package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import com.github.curiousoddman.receipt.parsing.utils.Utils;
import com.github.curiousoddman.receipt.parsing.validation.ItemNumbersValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.parseDateTime;
import static com.github.curiousoddman.receipt.parsing.utils.Patterns.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class RimiText2Receipt extends BasicText2Receipt<RimiContext> {
    public static final MyBigDecimal RECEIPT_NUMBER_ZERO = new MyBigDecimal(BigDecimal.ZERO, null, null);

    private final MyTesseract          tesseract;
    private final ItemNumbersValidator itemNumbersValidator;

    @Override
    protected RimiContext getContext(MyTessResult tessResult) {
        return new RimiContext(tessResult.getInputFile(), tessResult.getPlainText(), tessResult.getTsvDocument());
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
        String savingsAmount = getFirstGroup(context, SAVINGS_AMOUNT_SEARCH);
        if (savingsAmount == null) {
            return RECEIPT_NUMBER_ZERO;
        }

        return getReceiptNumber(context, savingsAmount);
    }

    @Override
    protected MyBigDecimal getTotalPayment(RimiContext context) {
        Optional<String> paymentAmount = getFirstGroup(context, PAYMENT_SUM);
        Optional<String> totalAmount = getFirstGroup(context, TOTAL_AMOUNT);
        Optional<String> bankCardAmount = getFirstGroup(context, BANK_CARD_AMOUNT);
        return ConversionUtils.getReceiptNumber(
                paymentAmount.orElse(null),
                totalAmount.orElse(null),
                bankCardAmount.orElse(null)
        );
    }

    private static Optional<String> getFirstGroup(RimiContext context, Pattern pattern) {
        return ConversionUtils.getFirstGroup(context.getLineMatching(pattern, 0), pattern);
    }

    @Override
    protected MyBigDecimal getShopBrandMoneyAccumulated(RimiContext context) {
        List<String> linesToMatch = List.of(
                "Nopelnītā Mans Rimi nauda",
                "Mans Rimi naudas uzkrājums"
        );

        for (String text : linesToMatch) {
            String line = context.getLineContaining(text, 0);
            if (line != null) {
                return ConversionUtils
                        .getBigDecimalAfterToken(line, text)
                        .orElse(new MyBigDecimal(null, null, "Big decimal cannot be parsed"));
            }
        }

        return new MyBigDecimal(null, null, "Unable to find shop brand money accumulated on receipt");
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
                String beforeSpace = finalCostGroupValue.substring(0, indexOfSpace);
                String afterSpace = finalCostGroupValue.substring(indexOfSpace + 1);
                finalCost = getReceiptNumber(context, beforeSpace, afterSpace);
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

    private MyBigDecimal getReceiptNumber(RimiContext context, String value) {
        try {
            return ConversionUtils.getReceiptNumber(value);
        } catch (Exception e) {
            List<TsvWord> tessWords = context.getTessWords(value);
            if (tessWords.size() == 1) {
                TsvWord tsvWord = tessWords.get(0);
                String reOcredText = null;
                try {
                    reOcredText = tesseract.doOCR(context.getOriginalFile(), tsvWord.getWordRect());
                } catch (TesseractException ex) {
                    return new MyBigDecimal(null, null, ex.getMessage());
                }
                return ConversionUtils.getReceiptNumber(reOcredText);
            } else {
                printUnableToFindTessWordsError(tessWords);
            }
            return new MyBigDecimal(null, null, e.getMessage());
        }
    }

    private static void printUnableToFindTessWordsError(List<TsvWord> tessWords) {
        log.error("Cannot find tess word: {}", tessWords.size());
        for (TsvWord tessWord : tessWords) {
            log.error("\t{}", tessWord);
        }
    }

    private MyBigDecimal getReceiptNumber(RimiContext context, String value, String anotherPart) {
        try {
            return ConversionUtils.getReceiptNumber(value);
        } catch (Exception e) {
            List<TsvWord> tessWords = context.getTessWords(value);
            List<TsvWord> anotherTessWords = context.getTessWords(anotherPart);

            Map<TsvWord, TsvWord> wordsThatFollow = new HashMap<>();

            for (TsvWord tessWord : tessWords) {
                findFollowingWord(tessWord, anotherTessWords, wordsThatFollow);
            }

            if (wordsThatFollow.size() == 1) {
                Iterator<Map.Entry<TsvWord, TsvWord>> iterator = wordsThatFollow.entrySet().iterator();
                Map.Entry<TsvWord, TsvWord> theOnlyValue = iterator.next();
                TsvWord firstWord = theOnlyValue.getKey();
                TsvWord secondWord = theOnlyValue.getValue();
                Rectangle bothWordsRectangle = Utils.uniteRectangles(firstWord.getWordRect(), secondWord.getWordRect());
                String reOcredText = null;
                try {
                    reOcredText = tesseract.doOCR(context.getOriginalFile(), bothWordsRectangle);
                } catch (TesseractException ex) {
                    return new MyBigDecimal(null, null, ex.getMessage());
                }
                return ConversionUtils.getReceiptNumber(reOcredText);
            } else {
                log.error("Cannot find tess following words: {}", wordsThatFollow);
            }
            return new MyBigDecimal(null, null, e.getMessage());
        }
    }

    private static void findFollowingWord(TsvWord tessWord, List<TsvWord> anotherTessWords, Map<TsvWord, TsvWord> wordsThatFollow) {
        for (TsvWord anotherTessWord : anotherTessWords) {
            if (tessWord.isFollowedBy(anotherTessWord)) {
                wordsThatFollow.put(tessWord, anotherTessWord);
                return;
            }
        }
    }

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
            List<TsvWord> tessWords = context.getTessWords(rn.text());
            if (tessWords.size() == 1) {
                TsvWord tsvWord = tessWords.get(0);
                String newValue = null;
                try {
                    newValue = tesseract.doOCR(context.getOriginalFile(), tsvWord.getWordRect());
                } catch (TesseractException e) {
                    item.setErrorMessage(e.getMessage());
                    return item;
                }
                ReceiptItem itemCopy = item.toBuilder().build();
                setter.accept(itemCopy, getReceiptNumber(context, newValue));
                if (itemNumbersValidator.isItemValid(itemCopy)) {
                    return itemCopy;
                }
            } else {
                printUnableToFindTessWordsError(tessWords);
            }
        }

        log.info("Unable to fix the item");
        return item;
    }

    private record ReceiptNumberWithSetter(MyBigDecimal rn, BiConsumer<ReceiptItem, MyBigDecimal> setter) {

    }
}
