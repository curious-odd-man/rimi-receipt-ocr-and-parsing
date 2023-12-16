package com.github.curiousoddman.receipt.parsing.model;

import java.time.LocalDateTime;

public record MyLocalDateTime(LocalDateTime value,
                              String text,
                              String errorText) implements MyValue {
}
