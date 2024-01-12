package com.github.curiousoddman.receipt.parsing.validation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationResult {
    private final Class<?>     validatorClass;
    private final List<String> errors;

    public static ValidationResult success(Class<?> c) {
        return new ValidationResult(c, List.of());
    }

    public static ValidationResult failure(Class<?> c, Exception e) {
        return new ValidationResult(c, List.of(e.getMessage()));
    }

    public static ValidationResult failure(Class<?> c, String text) {
        return new ValidationResult(c, List.of(text));
    }

    public static ValidationResult failure(Class<?> c, List<String> texts) {
        return new ValidationResult(c, texts);
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }
}
