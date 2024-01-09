package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.parsing.NumberOcrResult;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
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
    private final Tsv2Struct  tsv2Struct;

    public NumberOcrResult parse(TsvWord originalWord, int wordIndexInLine) {
        String value = originalWord.getText();
        // First attempt to parse big decimal as is
        if (isFormatValid(expectedFormat, value)) {
            return NumberOcrResult.of(toMyBigDecimal(value), originalWord.getWordRect());
        }

        triedValues.add("original: " + value);
        return combineWithNextWord(originalWord, wordIndexInLine);
    }

    private NumberOcrResult combineWithNextWord(TsvWord originalWord, int wordIndexInLine) {
        String value = originalWord.getText();
        // Sometimes there is extra space wrongly detected: -0, 36
        // Try to combine those into one and use it as a value
        TsvLine parentLine = originalWord.getParentLine();
        Optional<TsvWord> wordByWordNum = parentLine.getWordByWordNum(originalWord.getWordNum() + 1);
        if (wordByWordNum.isPresent()) {
            TsvWord tsvWord = wordByWordNum.get();
            String combinedWords = value + tsvWord.getText();
            if (isFormatValid(expectedFormat, combinedWords)) {
                Rectangle wordRect = new Rectangle(originalWord.getWordRect());
                wordRect.add(tsvWord.getWordRect());
                return NumberOcrResult.of(
                        toMyBigDecimal(combinedWords),
                        wordRect,
                        1);
            }

            triedValues.add("original combined with next: " + combinedWords);
        }

        return reOcrWordLocation(originalWord, wordIndexInLine);
    }

    private NumberOcrResult reOcrWordLocation(TsvWord originalWord, int wordIndexInLine) {
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

    private NumberOcrResult reOcrWordLine(TsvWord originalWord, int wordIndexInLine) {
        TsvLine line = null;
        String text;
        try {
            OcrConfig ocrConfig = OcrConfig
                    .builder(context.getOriginFile().preprocessedTiff())
                    .ocrArea(originalWord.getParentLine().getRectangle())
                    .ocrToTsv(true)
                    .build();
            // Finally try to re-ocr whole line and get the word by the same word num.
            String tsvText = context.getTesseract().doOCR(ocrConfig);
            TsvDocument tsvDocument = tsv2Struct.parseTsv(tsvText);
            List<TsvLine> lines = tsvDocument.getLines();
            line = lines.get(lines.size() - 1);     // Rectangle of line is streched up, touching previous line, that appears here as well.
            Optional<TsvWord> wordByWordNum = line.getWordByIndex(wordIndexInLine);
            if (wordByWordNum.isPresent()) {
                TsvWord tsvWord = wordByWordNum.get();
                text = tsvWord.getText();
                if (isFormatValid(expectedFormat, text)) {
                    return NumberOcrResult.of(toMyBigDecimal(text), tsvWord.getWordRect());
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
