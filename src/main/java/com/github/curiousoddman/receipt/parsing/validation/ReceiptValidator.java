package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;

public interface ReceiptValidator {
    ValidationStatus validate(Receipt receipt);

    default int getPriority() {
        return 0;
    }
}
