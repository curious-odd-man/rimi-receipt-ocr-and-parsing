package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ValidationExecutor {
    private final List<ReceiptValidator> receiptValidators;

    public void execute(ValidationStatsCollector validationStatsCollector, Receipt receipt) {
        try {
            List<ValidationResult> validationResult = new ArrayList<>();
            for (ReceiptValidator receiptValidator : receiptValidators) {
                validationResult.add(receiptValidator.validate(receipt));
            }
            if (validationResult.stream().allMatch(ValidationResult::isSuccess)) {
                validationStatsCollector.recordSuccess(receipt);
            } else {
                validationStatsCollector.recordFailure(receipt, validationResult);
            }
        } catch (Exception e) {
            validationStatsCollector.recordFailure(receipt, List.of(new ValidationResult(e)));
        }
    }
}
