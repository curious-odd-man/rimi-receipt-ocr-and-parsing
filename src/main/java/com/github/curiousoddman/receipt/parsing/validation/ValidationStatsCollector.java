package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ValidationStatsCollector {
    private final AtomicInteger successes = new AtomicInteger();
    private final AtomicInteger failures  = new AtomicInteger();

    public void recordSuccess() {
        successes.addAndGet(1);
        reportStats();
    }

    public void recordFailure(Receipt receipt, List<ValidationResult> validationResults) {
        failures.addAndGet(1);
        reportStats();
        log.error("Reporting errors for receipt {}", receipt.getFileName());
        validationResults
                .stream()
                .filter(vr -> !vr.isSuccess())
                .forEach(vr -> {
                    for (String error : vr.getErrors()) {
                        log.error("\t{} : {}", vr.getValidatorClass().getSimpleName(), error);
                    }
                });
    }

    private void reportStats() {
        log.info("Successes {}; Failures {};", successes, failures);
    }
}
