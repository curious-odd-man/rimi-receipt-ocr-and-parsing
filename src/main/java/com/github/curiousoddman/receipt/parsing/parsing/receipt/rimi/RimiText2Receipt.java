package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import com.github.curiousoddman.receipt.parsing.validation.ItemNumbersValidator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
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
    private final Tsv2Struct           tsv2Struct;

    @Override
    protected RimiContext getContext(MyTessResult tessResult) {
        return new RimiContext(tessResult.getInputFile(), tessResult.getTsvDocument());
    }

    @Override
    protected String getShopBrand(RimiContext context) {
        return "Rimi";
    }

    @Override
    protected String getShopName(RimiContext context) {
        List<TsvLine> lineContaining = context.getNextLinesAfterMatching(JUR_ADDR, 1);
        return lineContaining.get(0).getText();
    }

    @Override
    protected String getCashRegisterNumber(RimiContext context) {
        return context.getLineContaining("Kase Nr", 0).getText();
    }

    @Override
    protected MyBigDecimal getTotalSavings(RimiContext context) {
        return getWordFromMatchingLine(context, SAVINGS_AMOUNT_SEARCH, 3)
                .map(s -> getReceiptNumber(s, MONEY_AMOUNT, context))
                .orElse(RECEIPT_NUMBER_ZERO);
    }

    private static Optional<TsvWord> getWordFromMatchingLine(RimiContext context, Pattern pattern, int wordIndex) {
        return context
                .getLineMatching(pattern, 0)
                .flatMap(tsvLine -> tsvLine.getWordByWordNum(wordIndex));
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
        Optional<TsvLine> lineMatching = context.getLineMatching(pattern, 0);
        return lineMatching.flatMap(line -> ConversionUtils.getFirstGroup(line.getText(), pattern));
    }

    @Override
    protected MyBigDecimal getShopBrandMoneyAccumulated(RimiContext context) {
        List<String> linesToMatch = List.of(
                "Nopelnītā Mans Rimi nauda",
                "Mans Rimi naudas uzkrājums"
        );

        for (String text : linesToMatch) {
            TsvLine line = context.getLineContaining(text, 0);
            if (line != null) {
                return line
                        .getWordByWordNum(5)
                        .map(word -> ConversionUtils.getReceiptNumber(word.getText()))
                        .orElse(new MyBigDecimal(null, null, "Big decimal cannot be parsed"));
            }
        }

        return new MyBigDecimal(null, null, "Unable to find shop brand money accumulated on receipt");
    }

    @Override
    protected String getDocumentNumber(RimiContext context) {
        String text = "Dok. Nr.:";
        TsvLine line = context.getLineContaining(text, 0);
        return line.getText().split(text)[0].trim();
    }

    @Override
    protected LocalDateTime getReceiptDateTime(RimiContext context) {
        String time = getFirstGroup(context, RECEIPT_TIME_PATTERN).orElseThrow();
        return parseDateTime(time);
    }

    @Override
    protected Collection<? extends ReceiptItem> getItems(RimiContext context) {
        List<ReceiptItem> items = new ArrayList<>();
        List<TsvLine> linesBetween = context.getLinesBetween("KLIENTS:", "Maksājumu karte");
        linesBetween.add(TsvLine.dummy("HackLineThatDoesNotMatchAnyPatternButLetsUsProcessLastItemInList"));
        List<String> itemNameBuilder = new ArrayList<>();
        TsvLine priceLine = null;
        TsvLine discountLine = null;
        ItemLines itemLines = new ItemLines();
        for (TsvLine line : linesBetween) {

            if (line.isBlank()) {
                continue;
            }


            if (COUNT_PRICE_AND_SUM_LINE.matcher(line.getText()).matches()) {
                priceLine = line;
                itemLines.setPriceLine(line);
                continue;
            }

            if (priceLine == null) {
                itemNameBuilder.add(line.getText());
                itemLines.getDescriptionLines().add(line);
            } else {
                if (ITEM_DISCOUNT_LINE_PATTERN.matcher(line.getText()).matches()) {
                    discountLine = line;
                    itemLines.setDiscountLine(line);
                } else {
                    ReceiptItem item = createItem(context, discountLine, priceLine, itemNameBuilder);
//                    if (!itemNumbersValidator.isItemValid(item)) {
//                        item = tryOcrNumbersAgain(context, item, itemLines);
//                    }
                    items.add(item);
                    priceLine = null;
                    discountLine = null;
                    itemLines = new ItemLines();
                    itemLines.getDescriptionLines().add(line);
                    itemNameBuilder.add(line.getText());
                }
            }
        }
        return items;
    }

    private ReceiptItem createItem(RimiContext context,
                                   TsvLine discountLine,
                                   TsvLine priceLine,
                                   List<String> itemNameBuilder) {
        MyBigDecimal finalCost;
        MyBigDecimal discount = RECEIPT_NUMBER_ZERO;
        if (discountLine != null) {
            finalCost = getReceiptNumber(discountLine.getWordByWordNum(5), MONEY_AMOUNT, context);
            discount = getReceiptNumber(discountLine.getWordByWordNum(2), MONEY_AMOUNT, context);
        } else {
            Optional<TsvWord> finalCostGroupValue = priceLine.getWordByWordNum(6);
            finalCost = getReceiptNumber(finalCostGroupValue, MONEY_AMOUNT, context);
        }

        Optional<TsvWord> countText = priceLine.getWordByWordNum(1);
        Optional<TsvWord> pricePerUnitText = priceLine.getWordByWordNum(4);
        String unitsWord = priceLine.getWordByWordNum(2).orElseThrow().getText();
        ReceiptItem item = ReceiptItem
                .builder()
                .description(Strings.join(itemNameBuilder, ' ').trim())
                .count(getReceiptNumber(countText, unitsWord.toLowerCase().equals("gab") ? INTEGER : WEIGHT, context))
                .units(unitsWord)
                .pricePerUnit(getReceiptNumber(pricePerUnitText, MONEY_AMOUNT, context))
                .discount(discount)
                .finalCost(finalCost)
                .build();

        itemNameBuilder.clear();
        return item;
    }

    private MyBigDecimal getReceiptNumber(Optional<TsvWord> word, Pattern expectedFormat, RimiContext context) {
        return word
                .map(w -> getReceiptNumber(w, expectedFormat, context))
                .orElse(new MyBigDecimal(null, null, "Optional word is empty"));
    }

    private MyBigDecimal getReceiptNumber(TsvWord word, Pattern expectedFormat, RimiContext context) {
        List<String> values = new ArrayList<>();
        String value = word.getText();
        // First attempt to parse big decimal as is
        if (validateFormat(expectedFormat, value)) {
            return ConversionUtils.getReceiptNumber(value);
        }
        values.add(value);
        String text;
        try {
            // Next try to re-ocr the word itself
            text = tesseract.doOCR(context.getOriginalFile(), word.getWordRect(), false, true);
            if (validateFormat(expectedFormat, text)) {
                return ConversionUtils.getReceiptNumber(text);
            }
        } catch (TesseractException ex) {
            return new MyBigDecimal(null, null, ex.getMessage());
        }

        values.add(text);
        TsvLine line;
        try {
            // Finally try to re-ocr whole line and get the word by the same word num.
            String tsvText = tesseract.doOCR(context.getOriginalFile(), word.getParentLine().getRectangle(), true, false);
            TsvDocument tsvDocument = tsv2Struct.parseTsv(tsvText);
            line = tsvDocument.getLines().get(0);
            Optional<TsvWord> wordByWordNum = line.getWordByWordNum(word.getWordNum());
            if (wordByWordNum.isPresent()) {
                text = wordByWordNum.get().getText();
                if (validateFormat(expectedFormat, text)) {
                    return ConversionUtils.getReceiptNumber(text);
                }
            }
        } catch (Exception ex) {
            return new MyBigDecimal(null, null, ex.getMessage());
        }

        values.add("word by index " + word.getWordNum() + " from line " + line.getText());

        return new MyBigDecimal(null, null, "Value none of '" + values + "' does match expected format: " + expectedFormat.pattern());
    }

    private static boolean validateFormat(Pattern expectedFormat, String value) {
        String replaced = value
                .replace("\r", "")
                .replace("\n", "")
                .replace(',', '.');
        return expectedFormat
                .matcher(replaced)
                .matches();
    }

//    private ReceiptItem tryOcrNumbersAgain(RimiContext context, ReceiptItem item) {
//        List<ReceiptNumberWithSetter> allNumbers = List.of(
//                new ReceiptNumberWithSetter(item.getDiscount(), ReceiptItem::setDiscount),
//                new ReceiptNumberWithSetter(item.getFinalCost(), ReceiptItem::setFinalCost),
//                new ReceiptNumberWithSetter(item.getPricePerUnit(), ReceiptItem::setPricePerUnit),
//                new ReceiptNumberWithSetter(item.getCount(), ReceiptItem::setCount)
//        );
//        for (ReceiptNumberWithSetter rnWithSetter : allNumbers) {
//            MyBigDecimal rn = rnWithSetter.rn();
//            if (rn == RECEIPT_NUMBER_ZERO) {
//                continue;
//            }
//            BiConsumer<ReceiptItem, MyBigDecimal> setter = rnWithSetter.setter();
//            List<TsvWord> tessWords = context.getTessWords(rn.text());
//            if (tessWords.size() == 1) {
//                TsvWord tsvWord = tessWords.get(0);
//                String newValue = null;
//                try {
//                    newValue = tesseract.doOCR(context.getOriginalFile(), tsvWord.getWordRect());
//                } catch (TesseractException e) {
//                    item.setErrorMessage(e.getMessage());
//                    return item;
//                }
//                ReceiptItem itemCopy = item.toBuilder().build();
//                setter.accept(itemCopy, getReceiptNumber(newValue, MONEY_AMOUNT));
//                if (itemNumbersValidator.isItemValid(itemCopy)) {
//                    return itemCopy;
//                }
//            } else {
//                printUnableToFindTessWordsError(tessWords);
//            }
//        }
//
//        log.info("Unable to fix the item");
//        return item;
//    }

    @Data
    private static class ItemLines {
        List<TsvLine> descriptionLines = new ArrayList<>();
        TsvLine       priceLine;
        TsvLine       discountLine;
    }

    private record ReceiptNumberWithSetter(MyBigDecimal rn, BiConsumer<ReceiptItem, MyBigDecimal> setter) {

    }

    private static void printUnableToFindTessWordsError(List<TsvWord> tessWords) {
        log.error("Cannot find tess word: {}", tessWords.size());
        for (TsvWord tessWord : tessWords) {
            log.error("\t{}", tessWord);
        }
    }
}
