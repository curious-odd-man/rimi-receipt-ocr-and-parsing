package com.github.curiousoddman.receipt.parsing.model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;

public record MyBigDecimal(@JsonValue BigDecimal value, String text) {
}
