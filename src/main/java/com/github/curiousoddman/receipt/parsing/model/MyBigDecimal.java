package com.github.curiousoddman.receipt.parsing.model;

import java.math.BigDecimal;

public record MyBigDecimal(BigDecimal value,
                           String text,
                           String errorText) implements MyValue {
}
