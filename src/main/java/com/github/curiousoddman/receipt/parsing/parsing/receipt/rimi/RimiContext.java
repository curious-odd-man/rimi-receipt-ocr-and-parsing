package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.parsing.receipt.Context;
import com.github.curiousoddman.receipt.parsing.tess.MyTessWord;
import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Data
public class RimiContext implements Context {
    private final File             originalFile;
    private final String           rawReceiptText;
    private final String           tsvText;
    private final List<String>     rawReceiptLines;
    private final List<MyTessWord> tessWords;

    public RimiContext(File originalFile, String text, String tsvText) {
        rawReceiptText = text;
        this.tsvText = tsvText;
        rawReceiptLines = text.lines().toList();
        tessWords = tsvToTessWords(tsvText);
        this.originalFile = originalFile;
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
        return rawReceiptLines.stream().filter(line -> line.contains(text)).toList();
    }

    public List<String> getLinesMatching(Pattern pattern) {
        return rawReceiptLines.stream().filter(line -> pattern.matcher(line).matches()).toList();
    }

    public List<String> getNextLinesAfterMatching(Pattern pattern) {
        List<String> result = new ArrayList<>();
        Iterator<String> iterator = rawReceiptLines.iterator();
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
        Iterator<String> iterator = rawReceiptLines.iterator();
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

    public MyTessWord getTessWord(String text) {
        return tessWords
                .stream()
                .filter(mtw -> mtw.text().equals(text))
                .findAny()
                .orElseThrow();
    }

    public List<MyTessWord> getTessWords(String text) {
        return tessWords
                .stream()
                .filter(mtw -> mtw.text().equals(text))
                .toList();
    }

    public static List<MyTessWord> tsvToTessWords(String tsvText) {
        return tsvText
                .lines()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(line -> line.split("\t"))
                .map(RimiContext::getMyTessWord)
                .filter(Objects::nonNull)
                .toList();
    }

    private static MyTessWord getMyTessWord(String[] arr) {
        if (arr.length < 12) {
            return null;
        }
        return new MyTessWord(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]), Integer.parseInt(arr[2]), Integer.parseInt(arr[3]),
                              Integer.parseInt(arr[4]), Integer.parseInt(arr[5]), Integer.parseInt(arr[6]), Integer.parseInt(arr[7]),
                              Integer.parseInt(arr[8]), Integer.parseInt(arr[9]), Double.parseDouble(arr[10]), arr[11]);
    }
}
