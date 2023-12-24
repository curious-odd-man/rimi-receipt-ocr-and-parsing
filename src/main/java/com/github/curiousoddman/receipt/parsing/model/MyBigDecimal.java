package com.github.curiousoddman.receipt.parsing.model;

import java.math.BigDecimal;

public record MyBigDecimal(BigDecimal value,
                           String text,
                           String errorText) implements MyValue {

    public static MyBigDecimal zero() {
        return new MyBigDecimal(BigDecimal.ZERO, null, null);
    }

    public static MyBigDecimal error(String valueText, String errorText) {
        return new MyBigDecimal(null, valueText, errorText);
    }

    public static MyBigDecimal error(String errorText) {
        return new MyBigDecimal(null, null, errorText);
    }

    public static MyBigDecimal error(String valueText, Exception e) {
        return new MyBigDecimal(null, valueText, e.getMessage());
    }

    public static MyBigDecimal value(String numberText, String originalText) {
        return new MyBigDecimal(new BigDecimal(numberText), originalText, null);
    }
}
