package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.MyLocalDateTime;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.stats.ParsingStatsCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.tess.OcrConfig;
import com.github.curiousoddman.receipt.parsing.utils.Constants;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import com.github.curiousoddman.receipt.parsing.utils.Translations;
import com.github.curiousoddman.receipt.parsing.validation.ItemNumbersValidator;
import com.github.curiousoddman.receipt.parsing.validation.TotalAmountValidator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection.NO_CORRECTION;
import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.parseDateTime;
import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.toMyBigDecimal;
import static com.github.curiousoddman.receipt.parsing.utils.Patterns.*;
import static java.util.Objects.requireNonNullElse;

@Slf4j
@Component
@RequiredArgsConstructor
public class RimiText2Receipt extends BasicText2Receipt<RimiContext> {
    private static final Consumer<TsvWord> NOOP_CONSUMER = v -> {
    };

    private final MyTesseract          tesseract;
    private final Tsv2Struct           tsv2Struct;
    private final ItemNumbersValidator itemNumbersValidator;
    private final TotalAmountValidator totalAmountValidator;

    @Override
    protected Map<String, MyBigDecimal> getPaymentMethods(RimiContext context) {
        Map<String, MyBigDecimal> result = new LinkedHashMap<>();

        // Deposit coupon
        Optional<TsvLine> couponLine = context.getLineMatching(DEPOZIT_COUNPON_LINE, 0);
        couponLine
                .flatMap(tsvLine -> tsvLine.getWordByIndex(-1))
                .map(word -> getNumberFromReceiptAndReportError(word,
                                                                NUMBER_PATTERN,
                                                                context,
                                                                NOOP_CONSUMER,
                                                                NO_CORRECTION,
                                                                -1
                ))
                .map(NumberOcrResult::getNumber)
                .ifPresent(amount -> result.put(Constants.DEPOSIT_COUPON, amount));

        // Bank card amount
        MyBigDecimal bankCardTotalAmount = context
                .getLineMatching(TOTAL_CARD_AMOUNT, 0)
                .flatMap(l -> l.getWordByIndex(-2))
                .map(word -> getNumberFromReceiptAndReportError(word,
                                                                NUMBER_PATTERN,
                                                                context,
                                                                NOOP_CONSUMER,
                                                                NO_CORRECTION,
                                                                -2))
                .map(NumberOcrResult::getNumber)
                .orElse(null);

        MyBigDecimal bankCardAmount = context
                .getLineMatching(BANK_CARD_PAYMENT_AMOUNT, 0)
                .flatMap(l -> l.getWordByIndex(-1))
                .map(word -> getNumberFromReceiptAndReportError(word,
                                                                NUMBER_PATTERN,
                                                                context,
                                                                NOOP_CONSUMER,
                                                                NO_CORRECTION,
                                                                -1))
                .map(NumberOcrResult::getNumber)
                .orElse(null);

        if (bankCardAmount == null && bankCardTotalAmount == null) {
            result.put(Constants.BANK_CARD, MyBigDecimal.error("Both places are nulls"));
        } else if (bankCardAmount != null && bankCardTotalAmount != null) {
            if (bankCardAmount.isError() && bankCardTotalAmount.isError()) {
                result.put(Constants.BANK_CARD, MyBigDecimal.error(bankCardAmount.errorText() + "||||" + bankCardTotalAmount.errorText()));
            } else if (!bankCardAmount.isError() && !bankCardTotalAmount.isError()) {
                if (bankCardAmount.value().compareTo(bankCardTotalAmount.value()) == 0) {
                    result.put(Constants.BANK_CARD, bankCardAmount);
                } else {
                    result.put(Constants.BANK_CARD, MyBigDecimal.error("Amounts do not match: " + bankCardAmount.value() + " and " + bankCardTotalAmount.value()));
                }
            } else if (!bankCardAmount.isError()) {
                result.put(Constants.BANK_CARD, bankCardAmount);
            } else {
                result.put(Constants.BANK_CARD, bankCardTotalAmount);
            }
        } else {
            result.put(Constants.BANK_CARD, requireNonNullElse(bankCardAmount, bankCardTotalAmount));
        }

        return result;
    }

    @Override
    protected void validateAndFix(Receipt receipt, RimiContext context) {
        // TODO: see if this is helpful in the end
//        if (!totalAmountValidator.validate(receipt).isSuccess()) {
//            var bankCardAmount = context.getBankCardAmount().map(v -> reOcrWordInReceipt(v, MONEY_AMOUNT, context)).map(NumberOcrResult::getNumber);
//            var paymentAmount = context.getPaymentAmount().map(v -> reOcrWordInReceipt(v, MONEY_AMOUNT, context)).map(NumberOcrResult::getNumber);
//            var totalAmount = context.getTotalAmount().map(v -> reOcrWordInReceipt(v, MONEY_AMOUNT, context)).map(NumberOcrResult::getNumber);
//
//            MyBigDecimal value = ConversionUtils.pickMostFrequent(bankCardAmount.orElse(null), paymentAmount.orElse(null), totalAmount.orElse(null));
//            receipt.setTotalPayment(value);
//        }
    }

    @Override
    protected NumberOcrResult getDepositCouponPayment(RimiContext context) {
        Optional<TsvLine> couponLine = context.getLineMatching(Pattern.compile("Depoz.ta\\s+kupons\\s+"), 0);
        return couponLine
                .map(tsvLine -> getNumberFromReceiptAndReportError(tsvLine.getWordByIndex(-1),
                                                                   NUMBER_PATTERN,
                                                                   context,
                                                                   NOOP_CONSUMER,
                                                                   NO_CORRECTION,
                                                                   -1
                )).orElseGet(() -> NumberOcrResult.of(
                        MyBigDecimal.zero(),
                        null));
    }

    @Override
    protected Map<String, MyBigDecimal> getDiscounts(RimiContext context) {
        Map<String, MyBigDecimal> discounts = new LinkedHashMap<>();
        List<TsvLine> linesBetween = context.getLinesBetween("ATLAIDES", "Tavs ietaupījums");
        List<TsvWord> discountNameBuilder = new ArrayList<>();
        for (TsvLine tsvLine : linesBetween) {
            List<TsvWord> words = new ArrayList<>(tsvLine.getWords());
            TsvWord amountWord = words.get(words.size() - 1);
            NumberOcrResult amountNumber = getNumberFromReceiptAndReportError(amountWord,
                                                                              NUMBER_PATTERN,
                                                                              context,
                                                                              NOOP_CONSUMER,
                                                                              NO_CORRECTION,
                                                                              -1);
            if (amountNumber.isError()) {
                discountNameBuilder.addAll(tsvLine.getWords());
            } else {
                words.remove(words.size() - 1);
                discountNameBuilder.addAll(words);
                String name = discountNameBuilder.stream().map(TsvWord::getText).collect(Collectors.joining(" "));
                discounts.put(Translations.translateDiscountName(name), amountNumber.getNumber());
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
        List<TsvLine> lineContaining = context.getNextLinesAfterMatching(JUR_ADDR, 1);
        if (lineContaining.isEmpty()) {
            return "Error: Could not find line by pattern";
        }
        TsvLine tsvLine = lineContaining.get(0);
        context.collectShopNameLocation(tsvLine);
        return tsvLine.getText();
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
                .map(tsvWord -> getNumberFromReceiptAndReportError(tsvWord, MONEY_AMOUNT, context, context::collectTotalSavings, NO_CORRECTION, 2))
                .map(NumberOcrResult::getNumber)
                .orElse(MyBigDecimal.zero());
    }

    @Override
    protected MyBigDecimal getTotalAmount(RimiContext context) {
        Optional<TsvLine> optionalMatchingLine = context.getLineMatching(PAYMENT_SUM, 0);
        if (optionalMatchingLine.isEmpty()) {
            return MyBigDecimal.error("Failed to extract total amount");
        }

        TsvLine matchingLine = optionalMatchingLine.get();
        TsvWord lastWord = matchingLine.getWordByIndex(-1).orElseThrow();
        NumberOcrResult lastWordAsNumber = getNumberFromReceipt(lastWord,
                                                                MONEY_AMOUNT,
                                                                context,
                                                                NOOP_CONSUMER,
                                                                NO_CORRECTION,
                                                                -1);
        if (!lastWordAsNumber.isError()) {
            return lastWordAsNumber.getNumber();
        }

        Optional<TsvWord> optionalPreLastWord = matchingLine.getWordByIndex(-2);
        if (optionalPreLastWord.isEmpty()) {
            lastWordAsNumber.reportError();
            return MyBigDecimal.error("Failed to extract total amount: using 2 last words - there is only one word in line");
        }

        TsvWord preLastWord = optionalPreLastWord.get();
        if (preLastWord.getText().endsWith("EUR")) {
            lastWordAsNumber.reportError();
            return MyBigDecimal.error("Failed to extract total amount: using 2 last words - EUR is not a number");
        }

        Rectangle preLastRectangle = preLastWord.getWordRect();
        Rectangle lastRectangle = lastWord.getWordRect();
        Rectangle combinedRectangle = preLastRectangle.union(lastRectangle);

        OcrConfig ocrConfig = OcrConfig
                .builder(context.getOriginFile().preprocessedTiff())
                .ocrArea(combinedRectangle)
                .ocrDigitsOnly(true)
                .build();

        try {
            String text = tesseract.doOCR(ocrConfig);
            return toMyBigDecimal(text);
        } catch (TesseractException e) {
            lastWordAsNumber.reportError();
            return MyBigDecimal.error("Failed to extract total amount: using 2 last words - tesseract error", e);
        }
    }

    @Override
    protected MyBigDecimal getTotalPayment(RimiContext context) {
        Optional<TsvWord> paymentAmount = context.getLineMatching(PAYMENT_SUM, 0).flatMap(l -> l.getWordByIndex(-1));
        Optional<TsvWord> totalAmount = context.getLineMatching(TOTAL_CARD_AMOUNT, 0).flatMap(l -> l.getWordByIndex(-2));
        Optional<TsvWord> bankCardAmount = context.getLineMatching(BANK_CARD_PAYMENT_AMOUNT, 0).flatMap(l -> l.getWordByIndex(-1));
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
                        .orElse(MyBigDecimal.error("Big decimal cannot be parsed"));
            }
        }

        return MyBigDecimal.error("Unable to find shop brand money accumulated on receipt");
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
                .map(tsvLine -> getNumberFromReceiptAndReportError(
                        tsvLine.getWordByIndex(-1),
                        NUMBER_PATTERN,
                        context,
                        NOOP_CONSUMER,
                        NO_CORRECTION,
                        -1
                ))
                .map(NumberOcrResult::getNumber)
                .orElse(MyBigDecimal.zero());
    }

    private ReceiptItemResult createItem(RimiContext context,
                                         TsvLine discountLine,
                                         TsvLine priceLine,
                                         List<String> itemNameBuilder) {
        NumberOcrResult finalCostOcrResult;
        NumberOcrResult discountOcrResult = NumberOcrResult.of(MyBigDecimal.zero(), null);
        if (discountLine != null) {
            finalCostOcrResult = getNumberFromReceiptAndReportError(discountLine.getWordByIndex(-1), MONEY_AMOUNT, context, context::collectItemFinalCostWithDiscountLocation, NO_CORRECTION, -1);
            discountOcrResult = getNumberFromReceiptAndReportError(discountLine.getWordByWordNum(2), MONEY_AMOUNT, context, context::collectItemDiscountLocation, NO_CORRECTION, 1);
        } else {
            Optional<TsvWord> finalCostGroupValue = priceLine.getWordByWordNum(6);
            finalCostOcrResult = getNumberFromReceiptAndReportError(finalCostGroupValue, MONEY_AMOUNT, context, context::collectItemFinalCostLocation, NO_CORRECTION, 5);
        }

        Optional<TsvWord> countText = priceLine.getWordByWordNum(1);
        Optional<TsvWord> pricePerUnitText = priceLine.getWordByWordNum(4);
        TsvWord unitsTsvWord = priceLine
                .getWordByWordNum(2)
                .orElseThrow();
        String unitsWord = unitsTsvWord.getText();
        context.collectItemUnitsLocation(unitsTsvWord);
        NumberOcrResult countOcrResult = getNumberFromReceiptAndReportError(countText, unitsWord.equalsIgnoreCase("gab") ? INTEGER : WEIGHT, context, context::collectItemCountLocation, NO_CORRECTION, 0);
        NumberOcrResult pricePerUnitOcrResult = getNumberFromReceiptAndReportError(pricePerUnitText, MONEY_AMOUNT, context, context::collectPricePerUnitLocation, NO_CORRECTION, 3);
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

    private static Optional<TsvWord> getWordFromMatchingLine(RimiContext context, Pattern pattern, int wordIndex) {
        return context
                .getLineMatching(pattern, 0)
                .flatMap(tsvLine -> tsvLine.getWordByWordNum(wordIndex));
    }

    private static Optional<String> getFirstGroup(RimiContext context, Pattern pattern) {
        Optional<TsvLine> lineMatching = context.getLineMatching(pattern, 0);
        return lineMatching.flatMap(line -> ConversionUtils.getFirstGroup(line.getText(), pattern));
    }

    private NumberOcrResult getNumberFromReceiptAndReportError(Optional<TsvWord> word,
                                                               Pattern expectedFormat,
                                                               RimiContext context,
                                                               Consumer<TsvWord> tsvWordConsumer,
                                                               LocationCorrection locationCorrection,
                                                               int wordIndexInLine) {
        return word
                .map(w -> getNumberFromReceiptAndReportError(w, expectedFormat, context, tsvWordConsumer, locationCorrection, wordIndexInLine))
                .orElse(NumberOcrResult.ofError("Optional word is empty"));
    }

    private NumberOcrResult getNumberFromReceipt(TsvWord originalWord,
                                                 Pattern expectedFormat,
                                                 RimiContext context,
                                                 Consumer<TsvWord> tsvWordConsumer,
                                                 LocationCorrection locationCorrection,
                                                 int wordIndexInLine) {

        ReceiptNumberExtractionChain receiptNumberExtractionChain = new ReceiptNumberExtractionChain(
                expectedFormat,
                context,
                tsvWordConsumer,
                locationCorrection,
                tesseract,
                tsv2Struct
        );

        return receiptNumberExtractionChain.parse(originalWord, wordIndexInLine);
    }

    private NumberOcrResult getNumberFromReceiptAndReportError(TsvWord originalWord,
                                                               Pattern expectedFormat,
                                                               RimiContext context,
                                                               Consumer<TsvWord> tsvWordConsumer,
                                                               LocationCorrection locationCorrection,
                                                               int wordIndexInLine) {

        ReceiptNumberExtractionChain receiptNumberExtractionChain = new ReceiptNumberExtractionChain(
                expectedFormat,
                context,
                tsvWordConsumer,
                locationCorrection,
                tesseract,
                tsv2Struct
        );

        NumberOcrResult numberOcrResult = receiptNumberExtractionChain.parse(originalWord, wordIndexInLine);
        if (numberOcrResult.isError()) {
            numberOcrResult.reportError();
        }
        return numberOcrResult;
    }
//
//    private NumberOcrResult reOcrWordInReceipt(TsvWord originalWord,
//                                               Pattern expectedFormat,
//                                               RimiContext context) {
//
//        ReceiptNumberExtractionChain receiptNumberExtractionChain = new ReceiptNumberExtractionChain(
//                expectedFormat,
//                context,
//                NOOP_CONSUMER,
//                NO_CORRECTION,
//                tesseract,
//                tsv2Struct
//        );
//
//        return receiptNumberExtractionChain.reOcrWord(originalWord);
//    }
}
