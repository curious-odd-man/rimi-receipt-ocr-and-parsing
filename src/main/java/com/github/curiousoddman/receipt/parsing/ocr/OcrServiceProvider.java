package com.github.curiousoddman.receipt.parsing.ocr;

import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OcrServiceProvider {
    private final ThreadLocal<OcrService> tesseractThreadLocal;

    public OcrServiceProvider(Tsv2Struct tsv2Struct) {
        tesseractThreadLocal = ThreadLocal
                .withInitial(() -> {
                    log.info("New Tesseract created");
                    return new OcrService(tsv2Struct);
                });
    }

    public OcrService get() {
        return tesseractThreadLocal.get();
    }
}
