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
import com.github.curiousoddman.receipt.parsing.ocr.OcrResult;
import com.github.curiousoddman.receipt.parsing.ocr.OcrService;
import com.github.curiousoddman.receipt.parsing.ocr.OcrConfig;
import com.github.curiousoddman.receipt.parsing.utils.*;
import com.github.curiousoddman.receipt.parsing.validation.ItemNumbersValidator;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.*;
import static com.github.curiousoddman.receipt.parsing.utils.Patterns.*;
import static java.util.Objects.requireNonNullElse;

@Slf4j
@Component
@RequiredArgsConstructor
public class RimiText2Receipt {
    public static final int RECEIPT_WIDTH_PX      = 1441;
    public static final int HALF_RECEIPT_WIDTH_PX = RECEIPT_WIDTH_PX / 2;
    public static final int X_IMG_PX_MAX          = 1300;
    public static final int X_IMG_PX_MIN          = 1260;

    private final Tsv2Struct           tsv2Struct;
    private final ItemNumbersValidator itemNumbersValidator;

    public Receipt parse(String fileName,
                         OcrResult ocrResult,
                         OcrService ocrService) {
        RimiContext context = new RimiContext(
                ocrResult.originFile(),
                ocrResult.tsvDocument(),
                ocrService
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
                                                                MONEY_AMOUNT,
                                                                context,
                                                                -1
                ))
                .map(NumberOcrResult::getNumber)
                .forEach(amount -> result.add(Constants.DEPOSIT_COUPON, amount));

        // Bank card amount
        MyBigDecimal bankCardTotalAmount = context
                .getLineMatching(TOTAL_CARD_AMOUNT, 0)
                .flatMap(l -> l.getWordByIndex(-2))
                .map(word -> getNumberFromReceiptAndReportError(word,
                                                                MONEY_AMOUNT,
                                                                context,
                                                                -2
                ))
                .map(NumberOcrResult::getNumber)
                .orElse(null);

        MyBigDecimal bankCardAmount = context
                .getLineMatching(BANK_CARD_PAYMENT_AMOUNT, 0)
                .flatMap(l -> l.getWordByIndex(-1))
                .map(word -> getNumberFromReceiptAndReportError(word,
                                                                MONEY_AMOUNT,
                                                                context,
                                                                -1
                ))
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
            if (indexOfDiscountAmountEnd < X_IMG_PX_MAX && indexOfDiscountAmountEnd > X_IMG_PX_MIN) {
                NumberOcrResult amountNumber = getNumberFromReceiptAndReportError(amountWord,
                                                                                  MONEY_AMOUNT,
                                                                                  context,
                                                                                  -1
                );
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
        return tsvLine.getText();
    }

    protected String getCashRegisterNumber(RimiContext context) {
        TsvLine tsvLine = context.getLineContaining("Kase Nr", 0);
        return tsvLine.getText();
    }

    @SneakyThrows
    protected MyBigDecimal getTotalSavings(RimiContext context) {
        Optional<TsvWord> wordFromMatchingLine = getWordFromMatchingLine(context, SAVINGS_AMOUNT_SEARCH, 3);
        if (wordFromMatchingLine.isEmpty()) {
            return MyBigDecimal.zero();
        }

        TsvWord tsvWord = wordFromMatchingLine.get();
        TsvLine parentLine = tsvWord.getParentLine();
        OcrConfig routineRetryOcrConfig = OcrConfig
                .builder(context.getOriginFile().preprocessedTiff())
                .ocrDigitsOnly(true)
                .ocrArea(new Rectangle(HALF_RECEIPT_WIDTH_PX, parentLine.getY(), HALF_RECEIPT_WIDTH_PX, parentLine.getHeight()))
                .build();
        String reOcredText = context.getTesseract().doOCR(routineRetryOcrConfig);
        if (isFormatValid(MONEY_AMOUNT, reOcredText)) {
            return toMyBigDecimal(reOcredText);
        }

        return getNumberFromReceiptAndReportError(wordFromMatchingLine,
                                                  MONEY_AMOUNT,
                                                  context,
                                                  2
        )
                .getNumber();
    }

    @SneakyThrows
    protected MyBigDecimal getTotalAmount(RimiContext context) {
        Optional<TsvLine> optionalMatchingLine = context.getLineMatching(PAYMENT_SUM, 0);
        if (optionalMatchingLine.isEmpty()) {
            return MyBigDecimal.error("Could not find '" + PAYMENT_SUM + "' pattern on receipt!");
        }

        TsvLine matchingLine = optionalMatchingLine.get();
        OcrConfig routineRetryOcrConfig = OcrConfig
                .builder(context.getOriginFile().preprocessedTiff())
                .ocrDigitsOnly(true)
                .ocrArea(new Rectangle(HALF_RECEIPT_WIDTH_PX, matchingLine.getY(), HALF_RECEIPT_WIDTH_PX, matchingLine.getHeight()))
                .build();
        String reOcredText = context.getTesseract().doOCR(routineRetryOcrConfig);
        if (isFormatValid(MONEY_AMOUNT, reOcredText)) {
            return toMyBigDecimal(reOcredText);
        }
        return MyBigDecimal.error("Could not detect total amount");
    }

    protected MyBigDecimal getTotalPayment(RimiContext context) {
        Optional<TsvWord> paymentAmount = context.getLineMatching(PAYMENT_SUM, 0).flatMap(l -> l.getWordByIndex(-1));
        Optional<TsvWord> totalAmount = context.getLineMatching(TOTAL_CARD_AMOUNT, 0).flatMap(l -> l.getWordByIndex(-2));
        Optional<TsvWord> bankCardAmount = context.getLineMatching(BANK_CARD_PAYMENT_AMOUNT, 0).flatMap(l -> l.getWordByIndex(-1));
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
                        .map(word -> toMyBigDecimal(word.getText()))
                        .orElse(MyBigDecimal.error("Big decimal cannot be parsed"));
            }
        }

        return MyBigDecimal.error("Unable to find shop brand money accumulated on receipt");
    }

    protected String getDocumentNumber(RimiContext context) {
        String text = "Dok. Nr.:";
        TsvLine line = context.getLineContaining(text, 0);
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
                    boolean shouldIncludeItem = true;
                    ReceiptItemResult itemResult = createItem(context, discountLine, priceLine, itemNameBuilder);
                    if (itemResult.getReceiptItem().getDescription().contains("Korekci")) {
                        itemNameBuilder.remove(0);
                        String removedItemName = String.join(" ", itemNameBuilder);
                        List<ReceiptItem> allMatchingItems = items
                                .stream()
                                .filter(existingItem -> TextUtils.calculateLikeness(existingItem.getDescription(), removedItemName) >= 0.9)
                                .filter(existingItem -> !existingItem.isRemoved())
                                .toList();
                        if (allMatchingItems.size() == 1) {
                            allMatchingItems.forEach(i -> i.setRemoved(true));
                            shouldIncludeItem = false;
                        } else {
                            log.error("Correction error {}", allMatchingItems);
                            allMatchingItems.forEach(i -> i.setCorrectionItemError("This was detected as a correction for " + itemResult.getReceiptItem().getDescription()));
                        }
                    }
                    if (shouldIncludeItem) {
                        if (itemNumbersValidator.isItemValid(itemResult.getReceiptItem())) {
                            items.add(itemResult.getReceiptItem());
                        } else {
                            ReceiptItem item = tryOcrNumbersAgain(context, itemResult);
                            items.add(item);
                        }
                    }
                    priceLine = null;
                    discountLine = null;
                    itemLines = new ItemLines();
                    itemLines.getDescriptionLines().add(line);
                    itemNameBuilder.clear();
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
                        MONEY_AMOUNT,
                        context,
                        -1
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
        NumberOcrResult countOcrResult = getNumberFromReceiptAndReportError(countText, numberPattern, context, 0);
        Optional<TsvWord> pricePerUnitText = priceLine.getWordByWordNum(4 + countOcrResult.getSubsequntWordIndexOffset());
        NumberOcrResult pricePerUnitOcrResult = getNumberFromReceiptAndReportError(pricePerUnitText, MONEY_AMOUNT, context, 3 + countOcrResult.getSubsequntWordIndexOffset());

        NumberOcrResult finalCostOcrResult;
        NumberOcrResult discountOcrResult = NumberOcrResult.of(MyBigDecimal.zero(), null);
        if (discountLine != null) {
            finalCostOcrResult = getNumberFromReceiptAndReportError(discountLine.getWordByIndex(-1), MONEY_AMOUNT, context, -1);
            discountOcrResult = getNumberFromReceiptAndReportError(discountLine.getWordByWordNum(2), MONEY_AMOUNT, context, 1);
        } else {
            int additionalWordIndexOffset = countOcrResult.getSubsequntWordIndexOffset() + pricePerUnitOcrResult.getSubsequntWordIndexOffset();
            Optional<TsvWord> finalCostGroupValue = priceLine.getWordByWordNum(6 + additionalWordIndexOffset);
            finalCostOcrResult = getNumberFromReceiptAndReportError(finalCostGroupValue, MONEY_AMOUNT, context, 5 + additionalWordIndexOffset);
        }

        MyBigDecimal discountNumber = discountOcrResult.getNumber();
        if (discountNumber.value().compareTo(BigDecimal.ZERO) > 0) {
            discountNumber = new MyBigDecimal(discountNumber.value().multiply(BigDecimal.valueOf(-1)), discountNumber.text(), null);
        }
        ReceiptItem item = ReceiptItem
                .builder()
                .description(Strings.join(itemNameBuilder, ' ').trim())
                .count(countOcrResult.getNumber())
                .units(unitsWord)
                .pricePerUnit(pricePerUnitOcrResult.getNumber())
                .discount(discountNumber)
                .finalCost(finalCostOcrResult.getNumber())
                .build();

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
                new OcrResultWithSetter("Discount", receiptItemResult.getDiscountOcrResult(), ReceiptItem::setDiscount),
                new OcrResultWithSetter("Final Cost", receiptItemResult.getFinalCostOcrResult(), ReceiptItem::setFinalCost),
                new OcrResultWithSetter("Price per unit", receiptItemResult.getPricePerUnitOcrResult(), ReceiptItem::setPricePerUnit),
                new OcrResultWithSetter("Count", receiptItemResult.getCountOcrResult(), ReceiptItem::setCount)
        );
        for (OcrResultWithSetter rnWithSetter : allNumbers) {
            NumberOcrResult ocrResult = rnWithSetter.ocrResult();
            if (ocrResult.getLocation() == null) {
                log.error("Location is not present");
                continue;
            }
            BiConsumer<ReceiptItem, MyBigDecimal> setter = rnWithSetter.setter();
            Path path = refineLine(ocrResult.getLocation(), context);
            String newValue = null;
            try {
                OcrConfig ocrConfig;
                if (path == null) {
                    ocrConfig = OcrConfig
                            .builder(context.getOriginFile().preprocessedTiff())
                            .ocrDigitsOnly(true)
                            .ocrArea(ocrResult.getLocation())
                            .build();
                } else {
                    ocrConfig = OcrConfig
                            .builder(path)
                            .ocrDigitsOnly(true)
                            .build();
                }

                newValue = context.getTesseract().doOCR(ocrConfig);
            } catch (TesseractException e) {
                log.error("Failed to re-ocr item numbers", e);
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

    @SneakyThrows
    private static Path refineLine(Rectangle wordRect, RimiContext context) {
        Path path = context.getOriginFile().preprocessedTiff();
        BufferedImage inputImage = ImageIO.read(path.toFile());
        BufferedImage subimage = inputImage.getSubimage(wordRect.x, wordRect.y, wordRect.width, wordRect.height);
        BufferedImage bufferedImage = ImageUtils.getImageWithLineWithMostBlackPixels(subimage);
        if (bufferedImage == subimage) {
            return null;
        }
        Path rectangledFileName = Path.of(path + String.format("_%d_%d_%d_%d.tiff", wordRect.x, wordRect.y, wordRect.width, wordRect.height));
        ImageIO.write(bufferedImage, "tiff", rectangledFileName.toFile());
        return rectangledFileName;
    }

    @Data
    private static class ItemLines {
        List<TsvLine> descriptionLines = new ArrayList<>();
        TsvLine       priceLine;
        TsvLine       discountLine;
    }

    private record OcrResultWithSetter(String description, NumberOcrResult ocrResult, BiConsumer<ReceiptItem, MyBigDecimal> setter) {

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
                                                               int wordIndexInLine) {
        return word
                .map(w -> getNumberFromReceiptAndReportError(w,
                                                             expectedFormat,
                                                             context,
                                                             wordIndexInLine
                ))
                .orElse(NumberOcrResult.ofError("Optional word is empty"));
    }

    private NumberOcrResult getNumberFromReceipt(TsvWord originalWord,
                                                 Pattern expectedFormat,
                                                 RimiContext context,
                                                 int wordIndexInLine) {

        ReceiptNumberExtractionChain receiptNumberExtractionChain = new ReceiptNumberExtractionChain(
                expectedFormat,
                context,
                tsv2Struct
        );

        return receiptNumberExtractionChain.parse(originalWord, wordIndexInLine);
    }

    private NumberOcrResult getNumberFromReceiptAndReportError(TsvWord originalWord,
                                                               Pattern expectedFormat,
                                                               RimiContext context,
                                                               int wordIndexInLine) {

        ReceiptNumberExtractionChain receiptNumberExtractionChain = new ReceiptNumberExtractionChain(
                expectedFormat,
                context,
                tsv2Struct
        );

        NumberOcrResult numberOcrResult = receiptNumberExtractionChain.parse(originalWord, wordIndexInLine);
        if (numberOcrResult.isError()) {
            numberOcrResult.reportError();
        }
        return numberOcrResult;
    }
}
