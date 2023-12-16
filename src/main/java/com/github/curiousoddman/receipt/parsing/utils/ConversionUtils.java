package com.github.curiousoddman.receipt.parsing.utils;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.MyLocalDateTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.utils.Patterns.NON_DIGITS;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class ConversionUtils {

    public static MyBigDecimal getReceiptNumber(String text) {
        String cleanedInputText = text
                .replace("\r", "")
                .replace("\n", "")
                .replace(',', '.');
        try {
            return new MyBigDecimal(new BigDecimal(cleanedInputText), text, null);
        } catch (Exception e) {
            return new MyBigDecimal(null, text, e.getMessage());
        }
    }

    public static MyBigDecimal getReceiptNumberOrThrow(String text) {
        String cleanedInputText = text
                .replace("\r", "")
                .replace("\n", "")
                .replace(',', '.');
        return new MyBigDecimal(new BigDecimal(cleanedInputText), text, null);
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
                    return getReceiptNumberOrThrow(value);
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

    public static Optional<MyBigDecimal> getBigDecimalAfterToken(String line, String token) {
        String[] splitByProperty = line.split(token);
        for (String s : splitByProperty) {
            if (!s.isBlank()) {
                return Optional.of(getReceiptNumber(s));
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getFirstGroup(String line, Pattern pattern) {
        if (line != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    public static MyLocalDateTime parseDateTime(String text) {
        String yyyyMMddHHmmss = "yyyyMMddHHmmss";

        try {
            return new MyLocalDateTime(LocalDateTime.parse(
                    NON_DIGITS.matcher(text).replaceAll("").substring(0, yyyyMMddHHmmss.length()),
                    DateTimeFormatter.ofPattern(yyyyMMddHHmmss)),
                                       text,
                                       null
            );
        } catch (Exception e) {
            return new MyLocalDateTime(
                    null,
                    text,
                    e.getMessage()
            );
        }
    }
}
