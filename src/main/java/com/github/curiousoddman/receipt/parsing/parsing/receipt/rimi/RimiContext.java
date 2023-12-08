package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.parsing.receipt.Context;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

@Data
public class RimiContext implements Context {
    private final File        originalFile;
    private final String      rawReceiptText;
    private final TsvDocument tsvDocument;

    public RimiContext(File originalFile, String text, TsvDocument tsvDocument) {
        rawReceiptText = text;
        this.originalFile = originalFile;
        this.tsvDocument = tsvDocument;
    }

    public String getLineContaining(String text, int index) {
        List<String> linesContaining = getLinesContaining(text);
        if (linesContaining.size() > index) {
            return linesContaining.get(index);
        } else {
            return null;
        }
    }

    public String getLineMatching(Pattern pattern, int index) {
        List<String> linesContaining = getLinesMatching(pattern);
        if (linesContaining.size() > index) {
            return linesContaining.get(index);
        } else {
            return null;
        }
    }

    public List<String> getLinesContaining(String text) {
        return tsvDocument.getLines().stream().filter(line -> line.contains(text)).toList();
    }

    public List<String> getLinesMatching(Pattern pattern) {
        return tsvDocument.getLines().stream().filter(line -> pattern.matcher(line).matches()).toList();
    }

    public List<String> getNextLinesAfterMatching(Pattern pattern) {
        List<String> result = new ArrayList<>();
        Iterator<String> iterator = tsvDocument.getLines().iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (pattern.matcher(line).matches() && iterator.hasNext()) {
                result.add(iterator.next());
            }
        }
        return result;
    }

    public List<String> getLinesBetween(String beginning, String end) {
        List<String> result = new ArrayList<>();
        Iterator<String> iterator = tsvDocument.getLines().iterator();
        boolean addLines = false;
        while (iterator.hasNext()) {
            String line = iterator.next();
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

    @Override
    public String getText() {
        return rawReceiptText;
    }

    public TsvWord getTessWord(String text) {
        return tsvDocument
                .getWords()
                .stream()
                .filter(mtw -> mtw.getText().equals(text))
                .findAny()
                .orElseThrow();
    }

    public List<TsvWord> getTessWords(String text) {
        return tsvDocument
                .getWords()
                .stream()
                .filter(mtw -> mtw.getText().equals(text))
                .toList();
    }
}
