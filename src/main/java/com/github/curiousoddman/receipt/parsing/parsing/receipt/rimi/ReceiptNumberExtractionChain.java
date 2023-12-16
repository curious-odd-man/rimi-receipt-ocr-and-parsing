package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.tess.OcrConfig;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection.NO_CORRECTION;

@Slf4j
@RequiredArgsConstructor
public class ReceiptNumberExtractionChain {
    private final List<String> triedValues = new ArrayList<>();

    private final Pattern            expectedFormat;
    private final RimiContext        context;
    private final Consumer<TsvWord>  tsvWordConsumer;
    private final LocationCorrection locationCorrection;
    private final MyTesseract        tesseract;
    private final Tsv2Struct         tsv2Struct;

    public MyBigDecimal parse(TsvWord originalWord) {
        String value = originalWord.getText();
        // First attempt to parse big decimal as is
        if (validateFormat(expectedFormat, value)) {
            tsvWordConsumer.accept(originalWord);
            return ConversionUtils.getReceiptNumber(value);
        }

        triedValues.add(value);
        return reOcrWordLocation(originalWord);
    }

    private MyBigDecimal reOcrWordLocation(TsvWord originalWord) {
        String value = null;
        Rectangle originalWordRectangle = originalWord.getWordRect();
        try {
            OcrConfig ocrConfig = OcrConfig
                    .builder(context.getOriginFile().preprocessedTiff())
                    .ocrDigitsOnly(true)
                    .ocrArea(originalWordRectangle)
                    .build();
            value = tesseract.doOCR(ocrConfig);
            if (validateFormat(expectedFormat, value)) {
                tsvWordConsumer.accept(originalWord);
                return ConversionUtils.getReceiptNumber(value);
            }
        } catch (TesseractException ex) {
            log.error(ex.getMessage(), ex);
        }

        triedValues.add(value);

        return reOcrWordLocationInOriginalTiff(originalWord);
    }

    private MyBigDecimal reOcrWordLocationInOriginalTiff(TsvWord originalWord) {
        String value = null;
        Rectangle originalWordRectangle = originalWord.getWordRect();
        try {
            OcrConfig ocrConfig = OcrConfig
                    .builder(context.getOriginFile().convertedTiff())
                    .ocrDigitsOnly(true)
                    .ocrArea(originalWordRectangle)
                    .build();
            value = tesseract.doOCR(ocrConfig);
            if (validateFormat(expectedFormat, value)) {
                tsvWordConsumer.accept(originalWord);
                return ConversionUtils.getReceiptNumber(value);
            }
        } catch (TesseractException ex) {
            log.error(ex.getMessage(), ex);
        }

        triedValues.add(value);

        return reOcrWordLine(originalWord);
    }

    private MyBigDecimal reOcrWordLine(TsvWord originalWord) {
        TsvLine line = null;
        String text;
        try {
            OcrConfig ocrConfig = OcrConfig
                    .builder(context.getOriginFile().preprocessedTiff())
                    .ocrArea(originalWord.getParentLine().getRectangle())
                    .ocrToTsv(true)
                    .build();
            // Finally try to re-ocr whole line and get the word by the same word num.
            String tsvText = tesseract.doOCR(ocrConfig);
            TsvDocument tsvDocument = tsv2Struct.parseTsv(tsvText);
            line = tsvDocument.getLines().get(0);
            Optional<TsvWord> wordByWordNum = line.getWordByWordNum(originalWord.getWordNum());
            if (wordByWordNum.isPresent()) {
                TsvWord tsvWord = wordByWordNum.get();
                text = tsvWord.getText();
                if (validateFormat(expectedFormat, text)) {
                    tsvWordConsumer.accept(tsvWord);
                    return ConversionUtils.getReceiptNumber(text);
                }
            }
            triedValues.add("word by index " + originalWord.getWordNum() + " from line " + line.getText());
        } catch (Exception ex) {
            triedValues.add(ex.getMessage());
            log.error(ex.getMessage(), ex);
        }

        return reOcrWordLineInOriginalTiff(originalWord);
    }

    private MyBigDecimal reOcrWordLineInOriginalTiff(TsvWord originalWord) {
        TsvLine line = null;
        String text;
        try {
            OcrConfig ocrConfig = OcrConfig
                    .builder(context.getOriginFile().convertedTiff())
                    .ocrArea(originalWord.getParentLine().getRectangle())
                    .ocrToTsv(true)
                    .build();
            // Finally try to re-ocr whole line and get the word by the same word num.
            String tsvText = tesseract.doOCR(ocrConfig);
            TsvDocument tsvDocument = tsv2Struct.parseTsv(tsvText);
            line = tsvDocument.getLines().get(0);
            Optional<TsvWord> wordByWordNum = line.getWordByWordNum(originalWord.getWordNum());
            if (wordByWordNum.isPresent()) {
                TsvWord tsvWord = wordByWordNum.get();
                text = tsvWord.getText();
                if (validateFormat(expectedFormat, text)) {
                    tsvWordConsumer.accept(tsvWord);
                    return ConversionUtils.getReceiptNumber(text);
                }
            }
            triedValues.add("word by index " + originalWord.getWordNum() + " from line " + line.getText());
        } catch (Exception ex) {
            triedValues.add(ex.getMessage());
            log.error(ex.getMessage(), ex);
        }
        return reOcrByCorrection(originalWord);
    }

    private MyBigDecimal reOcrByCorrection(TsvWord originalWord) {
        Rectangle originalWordRectangle = originalWord.getWordRect();
        String text;
        if (locationCorrection != NO_CORRECTION) {
            // Additionally try to re-ocr by expected location based on statistics in locations-stats.txt
            Rectangle correctedLocation = locationCorrection.getCorrectedLocation(originalWordRectangle);
            try {
                OcrConfig ocrConfig = OcrConfig
                        .builder(context.getOriginFile().preprocessedTiff())
                        .ocrArea(correctedLocation)
                        .ocrDigitsOnly(true)
                        .build();
                text = tesseract.doOCR(ocrConfig);
                if (validateFormat(expectedFormat, text)) {
                    tsvWordConsumer.accept(originalWord);
                    return ConversionUtils.getReceiptNumber(text);
                }
            } catch (TesseractException ex) {
                return new MyBigDecimal(null, null, ex.getMessage());
            }

            triedValues.add("number by corrected location: " + text);
        }

        return reportError();
    }

    private MyBigDecimal reportError() {
        log.error("None of the values match the BigDecimal format");
        for (String triedValue : triedValues) {
            log.error("\t{}", triedValue);
        }
        log.error("----------------------");
        return new MyBigDecimal(null, null, "Tried " + triedValues.size() + " different value. No one matches BigDecimal format.");
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
}
