package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.parsing.receipt.Context;
import com.github.curiousoddman.receipt.parsing.tess.MyTessWord;
import com.github.curiousoddman.receipt.parsing.utils.ConversionUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

@Data
public class RimiContext implements Context {
    private final String       text;
    private final String       tsvText;
    private final List<String>     lines;
    private final List<MyTessWord> tessWords;

    public RimiContext(String text, String tsvText) {
        this.text = text;
        this.tsvText = tsvText;
        lines = text.lines().toList();
        tessWords = ConversionUtils.tsvToTessWords(tsvText);
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
        return lines.stream().filter(line -> line.contains(text)).toList();
    }

    public List<String> getLinesMatching(Pattern pattern) {
        return lines.stream().filter(line -> pattern.matcher(line).matches()).toList();
    }

    public List<String> getLinesAfterContaining(String text) {
        List<String> result = new ArrayList<>();
        Iterator<String> iterator = lines.iterator();
        while (iterator.hasNext()) {
            String line = iterator.next();
            if (line.contains(text) && iterator.hasNext()) {
                result.add(iterator.next());
            }
        }
        return result;
    }

    public String getLine(int index) {
        if (index >= 0) {
            return lines.get(index);
        } else {
            return lines.get(lines.size() + index);
        }
    }

    public List<String> getLinesBetween(String beginning, String end) {
        List<String> result = new ArrayList<>();
        Iterator<String> iterator = lines.iterator();
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
}
