package com.github.curiousoddman.receipt.parsing.utils;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.MyLocalDateTime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.curiousoddman.receipt.parsing.utils.Patterns.NON_DIGITS;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public class ConversionUtils {

    public static MyBigDecimal toMyBigDecimal(String text) {
        String cleanedInputText = cleanText(text);
        try {
            return MyBigDecimal.value(cleanedInputText, text);
        } catch (Exception e) {
            return MyBigDecimal.error(text, e);
        }
    }

    public static MyBigDecimal toMyBigDecimalOrThrow(String text) {
        String cleanedInputText = cleanText(text);
        return MyBigDecimal.value(cleanedInputText, text);
    }

    public static MyBigDecimal toMyBigDecimalMostFrequent(Pattern expectedFormat, String... texts) {
        Map<String, Long> countsPerText = Arrays
                .stream(texts)
                .filter(Objects::nonNull)
                .filter(text -> isFormatValid(expectedFormat, text))
                .collect(groupingBy(t -> t, counting()));

        TreeMap<Long, List<String>> frequencyToValuesMap = new TreeMap<>(Comparator.reverseOrder());
        countsPerText.forEach((k, v) -> frequencyToValuesMap
                .computeIfAbsent(v, ignore -> new ArrayList<>())
                .add(k));
        Exception suppressed = null;
        for (List<String> values : frequencyToValuesMap.values()) {
            for (String value : values) {
                try {
                    return toMyBigDecimalOrThrow(value);
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

    public static boolean isFormatValid(Pattern expectedFormat, String value) {
        String replaced = cleanText(value);
        return expectedFormat
                .matcher(replaced)
                .matches();
    }

    private static String cleanText(String value) {
        return value
                .replace("\r", "")
                .replace("\n", "")
                .replace(',', '.');
    }
}
