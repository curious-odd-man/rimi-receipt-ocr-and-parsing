package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.parsing.NumberOcrResult;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.TsvParser;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.document.OcrTsvResult;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.document.OcrResultLine;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.document.OcrResultWord;
import com.github.curiousoddman.receipt.parsing.ocr.OcrConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.isFormatValid;
import static com.github.curiousoddman.receipt.parsing.utils.ConversionUtils.toMyBigDecimal;

@Slf4j
@RequiredArgsConstructor
public class ReceiptNumberExtractionChain {
    private final List<String> triedValues = new ArrayList<>();

    private final Pattern     expectedFormat;
    private final RimiContext context;
    private final TsvParser   tsvParser;

    public NumberOcrResult parse(OcrResultWord originalWord, int wordIndexInLine) {
        String value = originalWord.getText();
        // First attempt to parse big decimal as is
        if (isFormatValid(expectedFormat, value)) {
            return NumberOcrResult.of(toMyBigDecimal(value), originalWord.getWordRect());
        }

        triedValues.add("original: " + value);
        return combineWithNextWord(originalWord, wordIndexInLine);
    }

    private NumberOcrResult combineWithNextWord(OcrResultWord originalWord, int wordIndexInLine) {
        String value = originalWord.getText();
        // Sometimes there is extra space wrongly detected: -0, 36
        // Try to combine those into one and use it as a value
        OcrResultLine parentLine = originalWord.getParentLine();
        Optional<OcrResultWord> wordByWordNum = parentLine.getWordByWordNum(originalWord.getWordNum() + 1);
        if (wordByWordNum.isPresent()) {
            OcrResultWord ocrResultWord = wordByWordNum.get();
            String combinedWords = value + ocrResultWord.getText();
            if (isFormatValid(expectedFormat, combinedWords)) {
                Rectangle wordRect = new Rectangle(originalWord.getWordRect());
                wordRect.add(ocrResultWord.getWordRect());
                return NumberOcrResult.of(
                        toMyBigDecimal(combinedWords),
                        wordRect,
                        1);
            }

            triedValues.add("original combined with next: " + combinedWords);
        }

        return reOcrWordLocation(originalWord, wordIndexInLine);
    }

    private NumberOcrResult reOcrWordLocation(OcrResultWord originalWord, int wordIndexInLine) {
        String value = null;
        Rectangle originalWordRectangle = originalWord.getWordRect();
        try {
            OcrConfig ocrConfig = OcrConfig
                    .builder(context.getOriginFile().preprocessedTiff())
                    .ocrDigitsOnly(true)
                    .ocrArea(originalWordRectangle)
                    .build();
            value = context.getTesseract().doOCR(ocrConfig);
            if (isFormatValid(expectedFormat, value)) {
                return NumberOcrResult.of(toMyBigDecimal(value), originalWordRectangle);
            }
        } catch (TesseractException ex) {
            log.error(ex.getMessage(), ex);
        }

        triedValues.add("re-ocr word: " + value);

        return reOcrWordLine(originalWord, wordIndexInLine);
    }

    private NumberOcrResult reOcrWordLine(OcrResultWord originalWord, int wordIndexInLine) {
        OcrResultLine line = null;
        String text;
        try {
            OcrConfig ocrConfig = OcrConfig
                    .builder(context.getOriginFile().preprocessedTiff())
                    .ocrArea(originalWord.getParentLine().getRectangle())
                    .ocrToTsv(true)
                    .build();
            // Finally try to re-ocr whole line and get the word by the same word num.
            String tsvText = context.getTesseract().doOCR(ocrConfig);
            OcrTsvResult ocrTsvResult = tsvParser.parse(tsvText);
            List<OcrResultLine> lines = ocrTsvResult.getLines();
            line = lines.get(lines.size() - 1);     // Rectangle of line is streched up, touching previous line, that appears here as well.
            Optional<OcrResultWord> wordByWordNum = line.getWordByIndex(wordIndexInLine);
            if (wordByWordNum.isPresent()) {
                OcrResultWord ocrResultWord = wordByWordNum.get();
                text = ocrResultWord.getText();
                if (isFormatValid(expectedFormat, text)) {
                    return NumberOcrResult.of(toMyBigDecimal(text), ocrResultWord.getWordRect());
                }
            }
            triedValues.add("re-ocr line: idx=" + originalWord.getWordNum() + "; line=" + line.getText());
        } catch (Exception ex) {
            triedValues.add(ex.getMessage());
            log.error(ex.getMessage(), ex);
        }

        return NumberOcrResult.ofError("Failed to extract number", triedValues);
    }
}
