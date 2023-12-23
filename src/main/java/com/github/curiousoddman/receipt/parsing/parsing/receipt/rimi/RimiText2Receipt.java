package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.*;
import com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.stats.ParsingStatsCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.tess.OcrConfig;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import com.github.curiousoddman.receipt.parsing.validation.ItemNumbersValidator;
import com.github.curiousoddman.receipt.parsing.validation.TotalAmountValidator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
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
import java.util.stream.Collectors;

import static com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection.NO_CORRECTION;
import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.toMyBigDecimal;
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
    protected NumberOcrResult getDepositCouponPayment(RimiContext context) {
        Optional<TsvLine> couponLine = context.getLineMatching(Pattern.compile("Depoz.ta\\s+kupons\\s+"), 0);
        return couponLine
                .map(tsvLine -> getNumberFromReceipt(tsvLine.getWordByIndex(-1),
                                                     NUMBER_PATTERN,
                                                     context,
                                                     word -> {
                                                     }, NO_CORRECTION
                )).orElseGet(() -> NumberOcrResult.of(
                        new MyBigDecimal(BigDecimal.ZERO, null, null),
                        null));
    }

    @Override
    protected List<Discount> getDiscounts(RimiContext context) {
        List<Discount> discounts = new ArrayList<>();
        List<TsvLine> linesBetween = context.getLinesBetween("ATLAIDES", "Tavs ietaupījums");
        List<TsvWord> discountNameBuilder = new ArrayList<>();
        for (TsvLine tsvLine : linesBetween) {
            List<TsvWord> words = new ArrayList<>(tsvLine.getWords());
            TsvWord amountWord = words.get(words.size() - 1);
            NumberOcrResult amountNumber = getNumberFromReceipt(amountWord,
                                                                NUMBER_PATTERN,
                                                                context,
                                                                w -> {
                                                                },
                                                                NO_CORRECTION);
            if (amountNumber.isError()) {
                discountNameBuilder.addAll(tsvLine.getWords());
            } else {
                words.remove(words.size() - 1);
                discountNameBuilder.addAll(words);
                String name = discountNameBuilder.stream().map(TsvWord::getText).collect(Collectors.joining(" "));
                discounts.add(new Discount(name, amountNumber.getNumber()));
                discountNameBuilder.clear();
            }
        }
        if (!discountNameBuilder.isEmpty()) {
            log.error("Wrongly parsed discount lines");
        }
        return discounts;
    }

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
                .map(tsvWord -> getNumberFromReceipt(tsvWord, MONEY_AMOUNT, context, context::collectTotalSavings, NO_CORRECTION))
                .map(NumberOcrResult::getNumber)
                .orElse(RECEIPT_NUMBER_ZERO);
    }

    @Override
    protected MyBigDecimal getTotalPayment(RimiContext context) {
        Optional<TsvWord> paymentAmount = context.getLineMatching(PAYMENT_SUM, 0).flatMap(l -> l.getWordByIndex(-1));
        Optional<TsvWord> totalAmount = context.getLineMatching(TOTAL_AMOUNT, 0).flatMap(l -> l.getWordByIndex(-2));
        Optional<TsvWord> bankCardAmount = context.getLineMatching(BANK_CARD_AMOUNT, 0).flatMap(l -> l.getWordByIndex(-1));
        context.setTotalAmountWords(paymentAmount, totalAmount, bankCardAmount);
        return toMyBigDecimal(
                MONEY_AMOUNT,
                paymentAmount.map(TsvWord::getText).orElse(null),
                totalAmount.map(TsvWord::getText).orElse(null),
                bankCardAmount.map(TsvWord::getText).orElse(null)
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
                            MyBigDecimal receiptNumber = toMyBigDecimal(word.getText());
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
        Optional<String> receiptTimeText = getFirstGroup(context, RECEIPT_TIME_PATTERN);
        if (receiptTimeText.isEmpty()) {
            return new MyLocalDateTime(null, "", "Counld not locate group for date/time");
        }
        return parseDateTime(receiptTimeText.orElseThrow());
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
                    ReceiptItemResult itemResult = createItem(context, discountLine, priceLine, itemNameBuilder);
                    if (itemNumbersValidator.isItemValid(itemResult.getReceiptItem())) {
                        items.add(itemResult.getReceiptItem());
                    } else {
                        ReceiptItem item = tryOcrNumbersAgain(context, itemResult);
                        items.add(item);
                    }
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
                .map(tsvLine -> getNumberFromReceipt(
                        tsvLine.getWordByIndex(-1),
                        NUMBER_PATTERN,
                        context,
                        tsvWord -> {
                        },
                        NO_CORRECTION
                ))
                .map(NumberOcrResult::getNumber)
                .orElse(new MyBigDecimal(BigDecimal.ZERO, null, null));
    }

    private ReceiptItemResult createItem(RimiContext context,
                                         TsvLine discountLine,
                                         TsvLine priceLine,
                                         List<String> itemNameBuilder) {
        NumberOcrResult finalCostOcrResult;
        NumberOcrResult discountOcrResult = NumberOcrResult.of(RECEIPT_NUMBER_ZERO, null);
        if (discountLine != null) {
            finalCostOcrResult = getNumberFromReceipt(discountLine.getWordByIndex(-1), MONEY_AMOUNT, context, context::collectItemFinalCostWithDiscountLocation, NO_CORRECTION);
            discountOcrResult = getNumberFromReceipt(discountLine.getWordByWordNum(2), MONEY_AMOUNT, context, context::collectItemDiscountLocation, NO_CORRECTION);
        } else {
            Optional<TsvWord> finalCostGroupValue = priceLine.getWordByWordNum(6);
            finalCostOcrResult = getNumberFromReceipt(finalCostGroupValue, MONEY_AMOUNT, context, context::collectItemFinalCostLocation, NO_CORRECTION);
        }

        Optional<TsvWord> countText = priceLine.getWordByWordNum(1);
        Optional<TsvWord> pricePerUnitText = priceLine.getWordByWordNum(4);
        TsvWord unitsTsvWord = priceLine
                .getWordByWordNum(2)
                .orElseThrow();
        String unitsWord = unitsTsvWord.getText();
        context.collectItemUnitsLocation(unitsTsvWord);
        NumberOcrResult countOcrResult = getNumberFromReceipt(countText, unitsWord.equalsIgnoreCase("gab") ? INTEGER : WEIGHT, context, context::collectItemCountLocation, NO_CORRECTION);
        NumberOcrResult pricePerUnitOcrResult = getNumberFromReceipt(pricePerUnitText, MONEY_AMOUNT, context, context::collectPricePerUnitLocation, NO_CORRECTION);
        ReceiptItem item = ReceiptItem
                .builder()
                .description(Strings.join(itemNameBuilder, ' ').trim())
                .count(countOcrResult.getNumber())
                .units(unitsWord)
                .pricePerUnit(pricePerUnitOcrResult.getNumber())
                .discount(discountOcrResult.getNumber())
                .finalCost(finalCostOcrResult.getNumber())
                .build();

        itemNameBuilder.clear();
        return new ReceiptItemResult(
                item,
                finalCostOcrResult,
                discountOcrResult,
                countOcrResult,
                pricePerUnitOcrResult

        );
    }

    private ReceiptItem tryOcrNumbersAgain(RimiContext context, ReceiptItemResult receiptItemResult) {
        List<OcrResultWithSetter> allNumbers = List.of(
                new OcrResultWithSetter(receiptItemResult.getDiscountOcrResult(), ReceiptItem::setDiscount),
                new OcrResultWithSetter(receiptItemResult.getFinalCostOcrResult(), ReceiptItem::setFinalCost),
                new OcrResultWithSetter(receiptItemResult.getPricePerUnitOcrResult(), ReceiptItem::setPricePerUnit),
                new OcrResultWithSetter(receiptItemResult.getCountOcrResult(), ReceiptItem::setCount)
        );
        for (OcrResultWithSetter rnWithSetter : allNumbers) {
            NumberOcrResult ocrResult = rnWithSetter.ocrResult();
            if (ocrResult.getLocation() == null) {
                continue;
            }
            BiConsumer<ReceiptItem, MyBigDecimal> setter = rnWithSetter.setter();

            String newValue = null;
            try {
                OcrConfig ocrConfig = OcrConfig
                        .builder(context.getOriginFile().preprocessedTiff())
                        .ocrDigitsOnly(true)
                        .ocrArea(ocrResult.getLocation())
                        .build();
                newValue = tesseract.doOCR(ocrConfig);
            } catch (TesseractException e) {
                receiptItemResult.getReceiptItem().setErrorMessage(e.getMessage());
                return receiptItemResult.getReceiptItem();
            }
            ReceiptItem itemCopy = receiptItemResult.getReceiptItem().toBuilder().build();
            setter.accept(itemCopy, toMyBigDecimal(newValue));
            if (itemNumbersValidator.isItemValid(itemCopy)) {
                return itemCopy;
            }
        }

        log.info("Unable to fix the item");
        return receiptItemResult.getReceiptItem();
    }

    @Data
    private static class ItemLines {
        List<TsvLine> descriptionLines = new ArrayList<>();
        TsvLine       priceLine;
        TsvLine       discountLine;
    }

    private record OcrResultWithSetter(NumberOcrResult ocrResult, BiConsumer<ReceiptItem, MyBigDecimal> setter) {

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

    private NumberOcrResult getNumberFromReceipt(Optional<TsvWord> word,
                                                 Pattern expectedFormat,
                                                 RimiContext context,
                                                 Consumer<TsvWord> tsvWordConsumer,
                                                 LocationCorrection locationCorrection) {
        return word
                .map(w -> getNumberFromReceipt(w, expectedFormat, context, tsvWordConsumer, locationCorrection))
                .orElse(NumberOcrResult.ofError("Optional word is empty"));
    }

    private NumberOcrResult getNumberFromReceipt(TsvWord originalWord,
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
}
