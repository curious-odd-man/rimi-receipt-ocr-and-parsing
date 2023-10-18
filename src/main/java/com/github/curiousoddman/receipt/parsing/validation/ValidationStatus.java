package com.github.curiousoddman.receipt.parsing.validation;

public enum ValidationStatus {
    SUCCESS,
    FAILURE;

    public ValidationStatus merge(ValidationStatus other) {
        if (this == SUCCESS && other == SUCCESS) {
            return SUCCESS;
        }
        return FAILURE;
    }
}
