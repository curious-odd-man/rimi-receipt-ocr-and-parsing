package com.github.curiousoddman.receipt.parsing.parsing;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.List;

@Data
@Slf4j
@RequiredArgsConstructor(staticName = "of")
public class NumberOcrResult {
    private final MyBigDecimal number;
    private final Rectangle    location;
    private final List<String> triedValues;

    public static NumberOcrResult of(MyBigDecimal number, Rectangle location) {
        return new NumberOcrResult(
                number,
                location,
                null
        );
    }

    public static NumberOcrResult ofError(String errorText) {
        return new NumberOcrResult(
                MyBigDecimal.error(errorText),
                null,
                null
        );
    }

    public static NumberOcrResult ofError(String errorText, List<String> triedValues) {
        return new NumberOcrResult(
                MyBigDecimal.error(errorText),
                null,
                triedValues
        );
    }

    public boolean isError() {
        return number.isError();
    }

    public void reportError() {
        if (triedValues != null) {
            log.error("None of the values match the BigDecimal format");
            for (String triedValue : triedValues) {
                log.error("\t{}", triedValue);
            }
            log.error("----------------------");
        }
    }
}