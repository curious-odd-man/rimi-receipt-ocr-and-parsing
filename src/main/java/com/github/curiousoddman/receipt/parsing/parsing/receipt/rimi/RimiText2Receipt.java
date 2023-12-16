package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.MyLocalDateTime;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.stats.ParsingStatsCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import com.github.curiousoddman.receipt.parsing.validation.ItemNumbersValidator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection.NO_CORRECTION;
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
    protected RimiContext getContext(MyTessResult tessResult, ParsingStatsCollector parsingStatsCollector) {
        return new RimiContext(
                tessResult.getOriginFile(),
                tessResult.getTsvDocument(),
                parsingStatsCollector
        );
    }

    @Override
    protected String getShopBrand(RimiContext context) {
        return "Rimi";
    }

    @Override
    protected String getShopName(RimiContext context) {
        try {
            List<TsvLine> lineContaining = context.getNextLinesAfterMatching(JUR_ADDR, 1);
            TsvLine tsvLine = lineContaining.get(0);
            context.collectShopNameLocation(tsvLine);
            return tsvLine.getText();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    @Override
    protected String getCashRegisterNumber(RimiContext context) {
        TsvLine tsvLine = context.getLineContaining("Kase Nr", 0);
        context.collectCashRegisterNumberLocation(tsvLine);
        return tsvLine.getText();
    }

    @Override
    protected MyBigDecimal getTotalSavings(RimiContext context) {
        return getWordFromMatchingLine(context, SAVINGS_AMOUNT_SEARCH, 3)
                .map(tsvWord -> getReceiptNumber(tsvWord, MONEY_AMOUNT, context, context::collectTotalSavings, NO_CORRECTION))
                .orElse(RECEIPT_NUMBER_ZERO);
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
                        .map(word -> {
                            MyBigDecimal receiptNumber = ConversionUtils.getReceiptNumber(word.getText());
                            if (!receiptNumber.isError()) {
                                context.collectShopBrandMoneyLocation(word);
                            }
                            return receiptNumber;
                        })
                        .orElse(new MyBigDecimal(null, null, "Big decimal cannot be parsed"));
            }
        }

        return new MyBigDecimal(null, null, "Unable to find shop brand money accumulated on receipt");
    }

    @Override
    protected String getDocumentNumber(RimiContext context) {
        String text = "Dok. Nr.:";
        TsvLine line = context.getLineContaining(text, 0);
        context.collectDocumentNumberLocation(line);
        return line.getText().split(text)[0].trim();
    }

    @Override
    protected MyLocalDateTime getReceiptDateTime(RimiContext context) {
        Optional<String> firstGroup = getFirstGroup(context, RECEIPT_TIME_PATTERN);
        if (firstGroup.isEmpty()) {
            return new MyLocalDateTime(null, "", "Counld not locate group for date/time");
        }
        return parseDateTime(firstGroup.orElseThrow());
    }

    @Override
    protected Collection<? extends ReceiptItem> getItems(RimiContext context) {
        List<ReceiptItem> items = new ArrayList<>();
        List<TsvLine> linesBetween = context.getLinesBetween("KLIENTS:", "Maksājumu karte");
        if (linesBetween.isEmpty()) {
            linesBetween = context.getLinesBetween("XXXXXXXXXXXXXXX3991", "Maksājumu karte");
        }
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

    @Override
    protected MyBigDecimal getUsedShopBrandMoney(RimiContext context) {
        Optional<TsvLine> lineMatching = context.getLineMatching(SHOP_BRAND_MONEY_SPENT, 0);
        return lineMatching
                .map(tsvLine -> getReceiptNumber(
                        tsvLine.getWordByIndex(-1),
                        NUMBER_PATTERN,
                        context,
                        tsvWord -> {
                        },
                        NO_CORRECTION
                )).orElse(new MyBigDecimal(BigDecimal.ZERO, null, null));
    }

    private ReceiptItem createItem(RimiContext context,
                                   TsvLine discountLine,
                                   TsvLine priceLine,
                                   List<String> itemNameBuilder) {
        MyBigDecimal finalCost;
        MyBigDecimal discount = RECEIPT_NUMBER_ZERO;
        if (discountLine != null) {
            finalCost = getReceiptNumber(discountLine.getWordByWordNum(5), MONEY_AMOUNT, context, context::collectItemFinalCostWithDiscountLocation, NO_CORRECTION);
            discount = getReceiptNumber(discountLine.getWordByWordNum(2), MONEY_AMOUNT, context, context::collectItemDiscountLocation, NO_CORRECTION);
        } else {
            Optional<TsvWord> finalCostGroupValue = priceLine.getWordByWordNum(6);
            finalCost = getReceiptNumber(finalCostGroupValue, MONEY_AMOUNT, context, context::collectItemFinalCostLocation, NO_CORRECTION);
        }

        Optional<TsvWord> countText = priceLine.getWordByWordNum(1);
        Optional<TsvWord> pricePerUnitText = priceLine.getWordByWordNum(4);
        TsvWord unitsTsvWord = priceLine
                .getWordByWordNum(2)
                .orElseThrow();
        String unitsWord = unitsTsvWord.getText();
        context.collectItemUnitsLocation(unitsTsvWord);
        ReceiptItem item = ReceiptItem
                .builder()
                .description(Strings.join(itemNameBuilder, ' ').trim())
                .count(getReceiptNumber(countText, unitsWord.toLowerCase().equals("gab") ? INTEGER : WEIGHT, context, context::collectItemCountLocation, NO_CORRECTION))
                .units(unitsWord)
                .pricePerUnit(getReceiptNumber(pricePerUnitText, MONEY_AMOUNT, context, context::collectPricePerUnitLocation, NO_CORRECTION))
                .discount(discount)
                .finalCost(finalCost)
                .build();

        itemNameBuilder.clear();
        return item;
    }

    private MyBigDecimal getReceiptNumber(Optional<TsvWord> word,
                                          Pattern expectedFormat,
                                          RimiContext context,
                                          Consumer<TsvWord> tsvWordConsumer,
                                          LocationCorrection locationCorrection) {
        return word
                .map(w -> getReceiptNumber(w, expectedFormat, context, tsvWordConsumer, locationCorrection))
                .orElse(new MyBigDecimal(null, null, "Optional word is empty"));
    }

    private MyBigDecimal getReceiptNumber(TsvWord originalWord,
                                          Pattern expectedFormat,
                                          RimiContext context,
                                          Consumer<TsvWord> tsvWordConsumer,
                                          LocationCorrection locationCorrection) {

        ReceiptNumberExtractionChain receiptNumberExtractionChain = new ReceiptNumberExtractionChain(
                expectedFormat,
                context,
                tsvWordConsumer,
                locationCorrection,
                tesseract,
                tsv2Struct
        );

        return receiptNumberExtractionChain.parse(originalWord);
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

    private static Optional<TsvWord> getWordFromMatchingLine(RimiContext context, Pattern pattern, int wordIndex) {
        return context
                .getLineMatching(pattern, 0)
                .flatMap(tsvLine -> tsvLine.getWordByWordNum(wordIndex));
    }

    private static Optional<String> getFirstGroup(RimiContext context, Pattern pattern) {
        Optional<TsvLine> lineMatching = context.getLineMatching(pattern, 0);
        return lineMatching.flatMap(line -> ConversionUtils.getFirstGroup(line.getText(), pattern));
    }
}
