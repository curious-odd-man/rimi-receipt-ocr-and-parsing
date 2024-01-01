package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.MyLocalDateTime;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.NumberOcrResult;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.ReceiptItemResult;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.stats.AllNumberCollector;
import com.github.curiousoddman.receipt.parsing.stats.ParsingStatsCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.tess.OcrConfig;
import com.github.curiousoddman.receipt.parsing.utils.Constants;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import com.github.curiousoddman.receipt.parsing.utils.ListValueHashMap;
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

import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.parseDateTime;
import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.toMyBigDecimal;
import static com.github.curiousoddman.receipt.parsing.utils.Patterns.*;
import static java.util.Objects.requireNonNullElse;

@Slf4j
@Component
@RequiredArgsConstructor
public class RimiText2Receipt {

    private static final Consumer<TsvWord> NOOP_CONSUMER = v -> {
    };

    private final Tsv2Struct           tsv2Struct;
    private final ItemNumbersValidator itemNumbersValidator;
    private final TotalAmountValidator totalAmountValidator;
    private final AllNumberCollector   allNumberCollector;

    public Receipt parse(String fileName,
                         MyTessResult myTessResult,
                         ParsingStatsCollector parsingStatsCollector,
                         MyTesseract myTesseract) {
        RimiContext context = new RimiContext(
                myTessResult.getOriginFile(),
                myTessResult.getTsvDocument(),
                parsingStatsCollector,
                myTesseract
        );
        Receipt receipt = Receipt
                .builder()
                .fileName(fileName)
                .shopBrand(getShopBrand(context))
                .shopName(getShopName(context))
                .cashRegisterNumber(getCashRegisterNumber(context))
                .totalSavings(getTotalSavings(context))
                .totalAmount(getTotalAmount(context))
                .totalPayment(getTotalPayment(context))
                .usedShopBrandMoney(getUsedShopBrandMoney(context))
                .shopBrandMoneyAccumulated(getShopBrandMoneyAccumulated(context))
                .documentNumber(getDocumentNumber(context))
                .receiptDateTime(getReceiptDateTime(context))
                .discounts(getDiscounts(context))
                .items(getItems(context))
                .paymentMethods(getPaymentMethods(context))
                .build();
        validateAndFix(receipt, context);
        return receipt;
    }

    protected Map<String, List<MyBigDecimal>> getPaymentMethods(RimiContext context) {
        ListValueHashMap<String, MyBigDecimal> result = new ListValueHashMap<>();

        // Deposit coupon
        List<TsvLine> couponLine = context.getLinesMatching(DEPOZIT_COUNPON_LINE);
        couponLine
                .stream()
                .map(tsvLine -> tsvLine.getWordByIndex(-1))
                .map(word -> getNumberFromReceiptAndReportError(word,
                                                                NUMBER_PATTERN,
                                                                context,
                                                                NOOP_CONSUMER,
                                                                -1,
                                                                "s"
                ))
                .map(NumberOcrResult::getNumber)
                .forEach(amount -> result.add(Constants.DEPOSIT_COUPON, amount));

        // Bank card amount
        MyBigDecimal bankCardTotalAmount = context
                .getLineMatching(TOTAL_CARD_AMOUNT, 0)
                .flatMap(l -> l.getWordByIndex(-2))
                .map(word -> getNumberFromReceiptAndReportError(word,
                                                                NUMBER_PATTERN,
                                                                context,
                                                                NOOP_CONSUMER,
                                                                -2,
                                                                "s"))
                .map(NumberOcrResult::getNumber)
                .orElse(null);

        MyBigDecimal bankCardAmount = context
                .getLineMatching(BANK_CARD_PAYMENT_AMOUNT, 0)
                .flatMap(l -> l.getWordByIndex(-1))
                .map(word -> getNumberFromReceiptAndReportError(word,
                                                                NUMBER_PATTERN,
                                                                context,
                                                                NOOP_CONSUMER,
                                                                -1,
                                                                "s"))
                .map(NumberOcrResult::getNumber)
                .orElse(null);

        if (bankCardAmount == null && bankCardTotalAmount == null) {
            result.add(Constants.BANK_CARD, MyBigDecimal.error("Both places are nulls"));
        } else if (bankCardAmount != null && bankCardTotalAmount != null) {
            if (bankCardAmount.isError() && bankCardTotalAmount.isError()) {
                result.add(Constants.BANK_CARD, MyBigDecimal.error(bankCardAmount.errorText() + "||||" + bankCardTotalAmount.errorText()));
            } else if (!bankCardAmount.isError() && !bankCardTotalAmount.isError()) {
                if (bankCardAmount.value().compareTo(bankCardTotalAmount.value()) == 0) {
                    result.add(Constants.BANK_CARD, bankCardAmount);
                } else {
                    result.add(Constants.BANK_CARD, MyBigDecimal.error("Amounts do not match: " + bankCardAmount.value() + " and " + bankCardTotalAmount.value()));
                }
            } else if (!bankCardAmount.isError()) {
                result.add(Constants.BANK_CARD, bankCardAmount);
            } else {
                result.add(Constants.BANK_CARD, bankCardTotalAmount);
            }
        } else {
            result.add(Constants.BANK_CARD, requireNonNullElse(bankCardAmount, bankCardTotalAmount));
        }

        return result;
    }

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

    protected Map<String, MyBigDecimal> getDiscounts(RimiContext context) {
        Map<String, MyBigDecimal> discounts = new LinkedHashMap<>();
        List<TsvLine> linesBetween = context.getLinesBetween("ATLAIDES", "Tavs ietaupījums");
        List<TsvWord> discountNameBuilder = new ArrayList<>();
        for (TsvLine tsvLine : linesBetween) {
            List<TsvWord> words = new ArrayList<>(tsvLine.getWords());
            TsvWord amountWord = words.get(words.size() - 1);
            int indexOfDiscountAmountEnd = amountWord.getX() + amountWord.getWidth();
            log.info("Discount amount end: {}", indexOfDiscountAmountEnd);

            if (indexOfDiscountAmountEnd < 1300 && indexOfDiscountAmountEnd > 1260) {
                NumberOcrResult amountNumber = getNumberFromReceiptAndReportError(amountWord,
                                                                                  NUMBER_PATTERN,
                                                                                  context,
                                                                                  NOOP_CONSUMER,
                                                                                  -1,
                                                                                  "s");
                words.remove(words.size() - 1);
                discountNameBuilder.addAll(words);
                String name = discountNameBuilder.stream().map(TsvWord::getText).collect(Collectors.joining(" "));
                discounts.put(Translations.translateDiscountName(name), amountNumber.getNumber());
                discountNameBuilder.clear();
            } else {
                discountNameBuilder.addAll(tsvLine.getWords());
            }
        }
        if (!discountNameBuilder.isEmpty()) {
            log.error("Wrongly parsed discount lines");
        }
        return discounts;
    }

    protected String getShopBrand(RimiContext context) {
        return "Rimi";
    }

    protected String getShopName(RimiContext context) {
        List<TsvLine> lineContaining = context.getNextLinesAfterMatching(JUR_ADDR, 1);
        if (lineContaining.isEmpty()) {
            return "Error: Could not find line by pattern";
        }
        TsvLine tsvLine = lineContaining.get(0);
        context.collectShopNameLocation(tsvLine);
        return tsvLine.getText();
    }

    protected String getCashRegisterNumber(RimiContext context) {
        TsvLine tsvLine = context.getLineContaining("Kase Nr", 0);
        context.collectCashRegisterNumberLocation(tsvLine);
        return tsvLine.getText();
    }

    protected MyBigDecimal getTotalSavings(RimiContext context) {
        return getWordFromMatchingLine(context, SAVINGS_AMOUNT_SEARCH, 3)
                .map(tsvWord -> getNumberFromReceiptAndReportError(tsvWord,
                                                                   MONEY_AMOUNT,
                                                                   context,
                                                                   context::collectTotalSavings,
                                                                   2,
                                                                   "L"))
                .map(NumberOcrResult::getNumber)
                .orElse(MyBigDecimal.zero());
    }

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
                                                                -1,
                                                                "XL");
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
            String text = context.getTesseract().doOCR(ocrConfig);
            return toMyBigDecimal(text);
        } catch (TesseractException e) {
            lastWordAsNumber.reportError();
            return MyBigDecimal.error("Failed to extract total amount: using 2 last words - tesseract error", e);
        }
    }

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

    protected String getDocumentNumber(RimiContext context) {
        String text = "Dok. Nr.:";
        TsvLine line = context.getLineContaining(text, 0);
        context.collectDocumentNumberLocation(line);
        return line.getText().split(text)[0].trim();
    }

    protected MyLocalDateTime getReceiptDateTime(RimiContext context) {
        Optional<String> receiptTimeText = getFirstGroup(context, RECEIPT_TIME_PATTERN);
        if (receiptTimeText.isEmpty()) {
            return new MyLocalDateTime(null, "", "Counld not locate group for date/time");
        }
        return parseDateTime(receiptTimeText.orElseThrow());
    }

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

    protected MyBigDecimal getUsedShopBrandMoney(RimiContext context) {
        Optional<TsvLine> lineMatching = context.getLineMatching(SHOP_BRAND_MONEY_SPENT, 0);
        return lineMatching
                .map(tsvLine -> getNumberFromReceiptAndReportError(
                        tsvLine.getWordByIndex(-1),
                        NUMBER_PATTERN,
                        context,
                        NOOP_CONSUMER,
                        -1,
                        "s"
                ))
                .map(NumberOcrResult::getNumber)
                .orElse(MyBigDecimal.zero());
    }

    private ReceiptItemResult createItem(RimiContext context,
                                         TsvLine discountLine,
                                         TsvLine priceLine,
                                         List<String> itemNameBuilder) {
        TsvWord unitsTsvWord = priceLine
                .getWordByWordNum(2)
                .orElseThrow();
        String unitsWord = unitsTsvWord.getText();
        Optional<TsvWord> countText = priceLine.getWordByWordNum(1);
        Pattern numberPattern = getNumberPattern(unitsWord);
        NumberOcrResult countOcrResult = getNumberFromReceiptAndReportError(countText, numberPattern, context, context::collectItemCountLocation, 0, "s");
        Optional<TsvWord> pricePerUnitText = priceLine.getWordByWordNum(4 + countOcrResult.getSubsequntWordIndexOffset());
        NumberOcrResult pricePerUnitOcrResult = getNumberFromReceiptAndReportError(pricePerUnitText, MONEY_AMOUNT, context, context::collectPricePerUnitLocation, 3 + countOcrResult.getSubsequntWordIndexOffset(), "s");

        NumberOcrResult finalCostOcrResult;
        NumberOcrResult discountOcrResult = NumberOcrResult.of(MyBigDecimal.zero(), null);
        if (discountLine != null) {
            finalCostOcrResult = getNumberFromReceiptAndReportError(discountLine.getWordByIndex(-1), MONEY_AMOUNT, context, context::collectItemFinalCostWithDiscountLocation, -1, "s");
            discountOcrResult = getNumberFromReceiptAndReportError(discountLine.getWordByWordNum(2), MONEY_AMOUNT, context, context::collectItemDiscountLocation, 1, "s");
        } else {
            int additionalWordIndexOffset = countOcrResult.getSubsequntWordIndexOffset() + pricePerUnitOcrResult.getSubsequntWordIndexOffset();
            Optional<TsvWord> finalCostGroupValue = priceLine.getWordByWordNum(6 + additionalWordIndexOffset);
            finalCostOcrResult = getNumberFromReceiptAndReportError(finalCostGroupValue, MONEY_AMOUNT, context, context::collectItemFinalCostLocation, 5 + additionalWordIndexOffset, "s");
        }

        context.collectItemUnitsLocation(unitsTsvWord);
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

    private static Pattern getNumberPattern(String unitsWord) {
        return switch (unitsWord.toLowerCase(Locale.ROOT)) {
            case "gab", "iep" -> INTEGER;
            default -> WEIGHT;
        };
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
                newValue = context.getTesseract().doOCR(ocrConfig);
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
                                                               int wordIndexInLine,
                                                               String type) {
        return word
                .map(w -> getNumberFromReceiptAndReportError(w,
                                                             expectedFormat,
                                                             context,
                                                             tsvWordConsumer,
                                                             wordIndexInLine,
                                                             type))
                .orElse(NumberOcrResult.ofError("Optional word is empty"));
    }

    private NumberOcrResult getNumberFromReceipt(TsvWord originalWord,
                                                 Pattern expectedFormat,
                                                 RimiContext context,
                                                 Consumer<TsvWord> tsvWordConsumer,
                                                 int wordIndexInLine,
                                                 String type) {

        ReceiptNumberExtractionChain receiptNumberExtractionChain = new ReceiptNumberExtractionChain(
                expectedFormat,
                context,
                tsvWordConsumer,
                tsv2Struct,
                allNumberCollector

        );

        return receiptNumberExtractionChain.parse(originalWord, wordIndexInLine, type);
    }

    private NumberOcrResult getNumberFromReceiptAndReportError(TsvWord originalWord,
                                                               Pattern expectedFormat,
                                                               RimiContext context,
                                                               Consumer<TsvWord> tsvWordConsumer,
                                                               int wordIndexInLine,
                                                               String type) {

        ReceiptNumberExtractionChain receiptNumberExtractionChain = new ReceiptNumberExtractionChain(
                expectedFormat,
                context,
                tsvWordConsumer,
                tsv2Struct,
                allNumberCollector
        );

        NumberOcrResult numberOcrResult = receiptNumberExtractionChain.parse(originalWord, wordIndexInLine, type);
        if (numberOcrResult.isError()) {
            numberOcrResult.reportError();
        }
        return numberOcrResult;
    }
}
