package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ValidationStatsCollector {
    private int successes;
    private int failures;

    public void recordSuccess(Receipt receipt) {
        successes += 1;
        reportStats();
        log.info("No validation errors for file {}", receipt.getFileName());
    }

    public void recordFailure(Receipt receipt, List<ValidationResult> validationResult) {
        failures += 1;
        reportStats();
        log.error("Reporting errors for receipt {}", receipt.getFileName());
        validationResult
                .stream()
                .filter(vr -> !vr.isSuccess())
                .forEach(vr -> {
                    for (Object error : vr.getErrors()) {
                        log.error("\t{} : {}", vr.getValidatorClass().getSimpleName(), error);
                    }
                });
    }

    private void reportStats() {
        log.info("Successes {}; Failures {};", successes, failures);
    }
}
