package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.document.OcrTsvResult;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.document.OcrResultLine;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.document.OcrResultWord;
import com.github.curiousoddman.receipt.parsing.ocr.OcrService;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Data
@RequiredArgsConstructor
public class RimiContext {
    private final OriginFile   originFile;
    private final OcrTsvResult ocrTsvResult;
    private final OcrService   tesseract;

    private Optional<OcrResultWord> paymentAmount;
    private Optional<OcrResultWord> totalAmount;
    private Optional<OcrResultWord> bankCardAmount;

    public OcrResultLine getLineContaining(String text, int index) {
        List<OcrResultLine> linesContaining = getLinesContaining(text);
        if (linesContaining.size() > index) {
            return linesContaining.get(index);
        } else {
            return null;
        }
    }

    public List<OcrResultLine> getNextLinesAfterMatching(Pattern pattern, int count) {
        List<OcrResultLine> result = new ArrayList<>();
        Iterator<OcrResultLine> iterator = ocrTsvResult.getLines().iterator();
        int found = -1;
        while (iterator.hasNext()) {
            OcrResultLine line = iterator.next();
            if (line.isBlank()) {
                continue;
            }

            if (found >= 0) {
                if (found == count) {
                    return result;
                } else {
                    found++;
                    result.add(line);
                }
            }

            if (pattern.matcher(line.getText()).matches()) {
                found = 0;
            }
        }
        return result;
    }

    public Optional<OcrResultLine> getLineMatching(Pattern pattern, int index) {
        List<OcrResultLine> linesContaining = getLinesMatching(pattern);
        if (linesContaining.size() > index) {
            return Optional.of(linesContaining.get(index));
        } else {
            return Optional.empty();
        }
    }

    public List<OcrResultLine> getLinesContaining(String text) {
        return ocrTsvResult
                .getLines()
                .stream()
                .filter(line -> line.contains(text))
                .toList();
    }

    public List<OcrResultLine> getLinesMatching(Pattern pattern) {
        return ocrTsvResult
                .getLines()
                .stream()
                .filter(line -> pattern.matcher(line.getText()).matches())
                .toList();
    }

    public List<OcrResultLine> getLinesBetween(String beginning, String end) {
        List<OcrResultLine> result = new ArrayList<>();
        Iterator<OcrResultLine> iterator = ocrTsvResult.getLines().iterator();
        boolean addLines = false;
        while (iterator.hasNext()) {
            OcrResultLine line = iterator.next();
            if (addLines) {
                if (line.contains(end)) {
                    return result;
                }
                result.add(line);
            } else {
                addLines = line.contains(beginning);
            }
        }
        return result;
    }
}
