package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.awt.*;

@Data
@RequiredArgsConstructor(staticName = "of")
public class NumberOcrResult {
    private final MyBigDecimal number;
    private final Rectangle    location;

    public static NumberOcrResult ofError(String errorText) {
        return new NumberOcrResult(
                new MyBigDecimal(null, null, errorText),
                null
        );
    }

    public boolean isError() {
        return number.isError();
    }
}
