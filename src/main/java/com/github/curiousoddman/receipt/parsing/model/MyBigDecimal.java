package com.github.curiousoddman.receipt.parsing.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;

public record MyBigDecimal(BigDecimal value,
                           String text,
                           String errorText) {
    public boolean isError() {
        return errorText != null;
    }

    @JsonValue
    public Object jsonValue() {
        return isError() ? errorText : value;
    }
}
