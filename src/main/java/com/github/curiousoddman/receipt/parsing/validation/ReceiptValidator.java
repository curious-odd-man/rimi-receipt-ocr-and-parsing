package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;

public interface ReceiptValidator {
    void validate(Receipt receipt);
}
