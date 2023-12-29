package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.parsing.LocationCorrection;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.stats.AllNumberCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.tess.OcrConfig;
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
import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.isFormatValid;
import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.toMyBigDecimal;

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
    private final AllNumberCollector allNumberCollector;

    public NumberOcrResult parse(TsvWord originalWord, int wordIndexInLine, String type) {
        String value = originalWord.getText();
        // First attempt to parse big decimal as is
        if (isFormatValid(expectedFormat, value)) {
            tsvWordConsumer.accept(originalWord);
            allNumberCollector.add(context.getOriginFile().preprocessedTiff(), type, originalWord);
            return NumberOcrResult.of(toMyBigDecimal(value), originalWord.getWordRect());
        }

        triedValues.add("original: " + value);
        return combineWithNextWord(originalWord, wordIndexInLine, type);
    }

    private NumberOcrResult combineWithNextWord(TsvWord originalWord, int wordIndexInLine, String type) {
        String value = originalWord.getText();
        // Sometimes there is extra space wrongly detected: -0, 36
        // Try to combine those into one and use it as a value
        TsvLine parentLine = originalWord.getParentLine();
        Optional<TsvWord> wordByWordNum = parentLine.getWordByWordNum(originalWord.getWordNum() + 1);
        if (wordByWordNum.isPresent()) {
            TsvWord tsvWord = wordByWordNum.get();
            String combinedWords = value + tsvWord.getText();
            if (isFormatValid(expectedFormat, combinedWords)) {
                tsvWordConsumer.accept(originalWord);
                Rectangle wordRect = new Rectangle(originalWord.getWordRect());
                wordRect.add(tsvWord.getWordRect());
                allNumberCollector.add(context.getOriginFile().preprocessedTiff(), type, combinedWords, wordRect);
                return NumberOcrResult.of(
                        toMyBigDecimal(combinedWords),
                        wordRect);
            }

            triedValues.add("original combined with next: " + combinedWords);
        }

        return reOcrWordLocation(originalWord, wordIndexInLine, type);
    }

    private NumberOcrResult reOcrWordLocation(TsvWord originalWord, int wordIndexInLine, String type) {
        String value = null;
        Rectangle originalWordRectangle = originalWord.getWordRect();
        try {
            OcrConfig ocrConfig = OcrConfig
                    .builder(context.getOriginFile().preprocessedTiff())
                    .ocrDigitsOnly(true)
                    .ocrArea(originalWordRectangle)
                    .build();
            value = tesseract.doOCR(ocrConfig);
            if (isFormatValid(expectedFormat, value)) {
                tsvWordConsumer.accept(originalWord);
                allNumberCollector.add(context.getOriginFile().preprocessedTiff(), type, value, originalWordRectangle);
                return NumberOcrResult.of(toMyBigDecimal(value), originalWordRectangle);
            }
        } catch (TesseractException ex) {
            log.error(ex.getMessage(), ex);
        }

        triedValues.add("re-ocr word: " + value);

        return reOcrWordLine(originalWord, wordIndexInLine, type);
    }

    private NumberOcrResult reOcrWordLine(TsvWord originalWord, int wordIndexInLine, String type) {
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
            List<TsvLine> lines = tsvDocument.getLines();
            line = lines.get(lines.size() - 1);     // Rectangle of line is streched up, touching previous line, that appears here as well.
            Optional<TsvWord> wordByWordNum = line.getWordByIndex(wordIndexInLine);
            if (wordByWordNum.isPresent()) {
                TsvWord tsvWord = wordByWordNum.get();
                text = tsvWord.getText();
                if (isFormatValid(expectedFormat, text)) {
                    tsvWordConsumer.accept(tsvWord);
                    allNumberCollector.add(context.getOriginFile().preprocessedTiff(), type, tsvWord);
                    return NumberOcrResult.of(toMyBigDecimal(text), tsvWord.getWordRect());
                }
            }
            triedValues.add("re-ocr line: idx=" + originalWord.getWordNum() + "; line=" + line.getText());
        } catch (Exception ex) {
            triedValues.add(ex.getMessage());
            log.error(ex.getMessage(), ex);
        }

        //return reOcrWordLineInOriginalTiff(originalWord, wordIndexInLine, type);
        return reOcrByCorrection(originalWord);
    }

    private NumberOcrResult reOcrByCorrection(TsvWord originalWord) {
        Rectangle originalWordRectangle = originalWord.getWordRect();
        String text = null;
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
                if (isFormatValid(expectedFormat, text)) {
                    tsvWordConsumer.accept(originalWord);
                    return NumberOcrResult.of(toMyBigDecimal(text), originalWordRectangle);
                }
            } catch (TesseractException ex) {
                triedValues.add(ex.getMessage());
                log.error(ex.getMessage(), ex);
            }

            triedValues.add("ocr corrected location: " + text);
        }

        return NumberOcrResult.ofError("Failed to extract number", triedValues);
    }
}
