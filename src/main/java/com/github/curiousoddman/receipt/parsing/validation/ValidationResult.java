package com.github.curiousoddman.receipt.parsing.validation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ValidationResult {
    private final Class<?>     validatorClass;
    private final List<Object> errors;

    public ValidationResult(Class<?> c) {
        validatorClass = c;
        errors = List.of();
    }

    public ValidationResult(Exception e) {
        validatorClass = e.getClass();
        errors = List.of(e);
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }
}
