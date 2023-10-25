package com.github.curiousoddman.receipt.parsing.utils;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class ConversionUtils {
    public static MyBigDecimal getReceiptNumber(String text) {
        String replaced = text
                .trim()
                .replace(',', '.')
                .replace(" ", ""); // Values like '0. 50'
        try {
            return new MyBigDecimal(new BigDecimal(replaced), text);
        } catch (Exception e) {
            throw new IllegalStateException("Error value '" + replaced + "'", e);
        }
    }

    /**
     * Converts ONE of texts into BigDecimal value.
     *
     * @param texts input text values
     * @return BigDecimal value
     */
    public static MyBigDecimal getReceiptNumber(String... texts) {
        Map<String, Long> countsPerText = Arrays
                .stream(texts)
                .filter(Objects::nonNull)
                .collect(groupingBy(t -> t, counting()));

        TreeMap<Long, List<String>> frequencyToValuesMap = new TreeMap<>(Comparator.reverseOrder());
        countsPerText.forEach((k, v) -> frequencyToValuesMap
                .computeIfAbsent(v, ignore -> new ArrayList<>())
                .add(k));
        Exception suppressed = null;
        for (List<String> values : frequencyToValuesMap.values()) {
            for (String value : values) {
                try {
                    return getReceiptNumber(value);
                } catch (Exception e) {
                    if (suppressed != null) {
                        e.addSuppressed(suppressed);
                    }
                    suppressed = e;
                }
            }
        }

        NoSuchElementException noSuchElementException = new NoSuchElementException("None of the texts can be parsed as BigDecimal: " + Arrays.toString(texts));
        if (suppressed != null) {
            noSuchElementException.addSuppressed(suppressed);
        }
        throw noSuchElementException;
    }

    public static MyBigDecimal getBigDecimalAfterToken(String line, String token) {
        String[] splitByProperty = line.split(token);
        for (String s : splitByProperty) {
            if (!s.isBlank()) {
                return getReceiptNumber(s);
            }
        }
        return null;
    }

    public static String getFirstGroup(String line, Pattern pattern) {
        if (line != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
