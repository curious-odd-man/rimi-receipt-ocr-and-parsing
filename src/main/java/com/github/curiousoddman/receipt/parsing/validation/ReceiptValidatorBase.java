package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class ReceiptValidatorBase implements ReceiptValidator {
    @Override
    public ValidationStatus validate(Receipt receipt) {
        try {
            List<Object> validationErrors = performValidation(receipt);
            if (validationErrors.isEmpty()) {
                return ValidationStatus.SUCCESS;
            }
            log.error("Receipt {} validation failed with following errors:", receipt.getFileName());
            for (Object validationError : validationErrors) {
                log.error("\t{}", validationError);
            }
        } catch (Exception e) {
            log.error("Receipt validation failed with exception.", e);
        }
        return ValidationStatus.FAILURE;
    }

    protected abstract List<Object> performValidation(Receipt receipt);
}
