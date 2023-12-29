package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ValidationExecutor {
    private final List<ReceiptValidator> receiptValidators;
    private final Map<String, Object>    resultMap = new ConcurrentHashMap<>();

    public void execute(ValidationStatsCollector validationStatsCollector, Receipt receipt) {
        List<ValidationResult> validationResult = new ArrayList<>();
        try {
            for (ReceiptValidator receiptValidator : receiptValidators) {
                validationResult.add(receiptValidator.validate(receipt));
            }
            if (validationResult.stream().allMatch(ValidationResult::isSuccess)) {
                validationStatsCollector.recordSuccess(receipt);
                resultMap.put(receipt.getFileName(), "SUCCESS");
            } else {
                validationStatsCollector.recordFailure(receipt, validationResult);
                resultMap.put(receipt.getFileName(), new Error("FAIL", getErrorsList(validationResult)));
            }
        } catch (Exception e) {
            validationStatsCollector.recordFailure(receipt, List.of(new ValidationResult(e)));
            resultMap.put(receipt.getFileName(), new Error("FAIL", getErrorsList(validationResult)));
        }
    }

    private static List<String> getErrorsList(List<ValidationResult> validationResult) {
        return validationResult
                .stream()
                .map(ValidationResult::getErrors)
                .flatMap(List::stream)
                .map(Object::toString)
                .filter(Strings::isNotBlank)
                .toList();
    }

    @SneakyThrows
    public void saveResult(Path path) {
        String json = JsonUtils.OBJECT_WRITER.writeValueAsString(resultMap);
        Files.writeString(path, json);
    }

    private record Error(String header, List<String> errors) {

    }
}
