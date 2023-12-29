package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.stats.ParsingStatsCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
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
    private final OriginFile            originFile;
    private final TsvDocument           tsvDocument;
    private final ParsingStatsCollector parsingStatsCollector;
    private final MyTesseract           tesseract;

    private Optional<TsvWord> paymentAmount;
    private Optional<TsvWord> totalAmount;
    private Optional<TsvWord> bankCardAmount;

    public TsvLine getLineContaining(String text, int index) {
        List<TsvLine> linesContaining = getLinesContaining(text);
        if (linesContaining.size() > index) {
            return linesContaining.get(index);
        } else {
            return null;
        }
    }

    public List<TsvLine> getNextLinesAfterMatching(Pattern pattern, int count) {
        List<TsvLine> result = new ArrayList<>();
        Iterator<TsvLine> iterator = tsvDocument.getLines().iterator();
        int found = -1;
        while (iterator.hasNext()) {
            TsvLine line = iterator.next();
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

    public Optional<TsvLine> getLineMatching(Pattern pattern, int index) {
        List<TsvLine> linesContaining = getLinesMatching(pattern);
        if (linesContaining.size() > index) {
            return Optional.of(linesContaining.get(index));
        } else {
            return Optional.empty();
        }
    }

    public List<TsvLine> getLinesContaining(String text) {
        return tsvDocument
                .getLines()
                .stream()
                .filter(line -> line.contains(text))
                .toList();
    }

    public List<TsvLine> getLinesMatching(Pattern pattern) {
        return tsvDocument
                .getLines()
                .stream()
                .filter(line -> pattern.matcher(line.getText()).matches())
                .toList();
    }

    public List<TsvLine> getLinesBetween(String beginning, String end) {
        List<TsvLine> result = new ArrayList<>();
        Iterator<TsvLine> iterator = tsvDocument.getLines().iterator();
        boolean addLines = false;
        while (iterator.hasNext()) {
            TsvLine line = iterator.next();
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

    public void collectShopNameLocation(TsvLine tsvLine) {
        parsingStatsCollector.collectShopNameLocation(originFile, tsvLine);
    }

    public void collectCashRegisterNumberLocation(TsvLine tsvLine) {
        parsingStatsCollector.collectCashRegisterNumberLocation(originFile, tsvLine);
    }

    public void collectTotalSavings(TsvWord tsvWord) {
        parsingStatsCollector.collectTotalSavings(originFile, tsvWord);
    }

    public void collectShopBrandMoneyLocation(TsvWord tsvWord) {
        parsingStatsCollector.collectShopBrandMoneyLocation(originFile, tsvWord);
    }

    public void collectDocumentNumberLocation(TsvLine line) {
        parsingStatsCollector.collectDocumentNumberLocation(originFile, line);
    }

    public void collectItemFinalCostLocation(TsvWord tsvWord) {
        parsingStatsCollector.collectItemFinalCostLocation(originFile, tsvWord);
    }

    public void collectItemDiscountLocation(TsvWord tsvWord) {
        parsingStatsCollector.collectItemDiscountLocation(originFile, tsvWord);
    }

    public void collectItemCountLocation(TsvWord tsvWord) {
        parsingStatsCollector.collectItemCountLocation(originFile, tsvWord);
    }

    public void collectPricePerUnitLocation(TsvWord tsvWord) {
        parsingStatsCollector.collectPricePerUnitLocation(originFile, tsvWord);
    }

    public void collectItemUnitsLocation(TsvWord tsvWord) {
        parsingStatsCollector.collectItemUnitsLocation(originFile, tsvWord);
    }

    public void collectItemFinalCostWithDiscountLocation(TsvWord tsvWord) {
        parsingStatsCollector.collectItemFinalCostWithDiscountLocation(originFile, tsvWord);
    }

    public void setTotalAmountWords(Optional<TsvWord> paymentAmount, Optional<TsvWord> totalAmount, Optional<TsvWord> bankCardAmount) {
        setPaymentAmount(paymentAmount);
        setTotalAmount(totalAmount);
        setBankCardAmount(bankCardAmount);
    }
}
