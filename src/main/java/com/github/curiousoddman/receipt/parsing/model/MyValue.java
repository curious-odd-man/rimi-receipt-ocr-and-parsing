package com.github.curiousoddman.receipt.parsing.model;

import com.fasterxml.jackson.annotation.JsonValue;

public interface MyValue {

    String errorText();

    Object value();

    default boolean isError() {
        return errorText() != null;
    }

    @JsonValue
    default Object jsonValue() {
        return isError()
               ? ("ERROR: " + errorText())
               : value();
    }
}
