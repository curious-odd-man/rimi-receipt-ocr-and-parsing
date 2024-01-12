package com.github.curiousoddman.receipt.parsing.ocr;

import com.github.curiousoddman.receipt.parsing.ocr.tsv.TsvParser;
import com.github.curiousoddman.receipt.parsing.utils.PathsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OcrServiceProvider {
    private final ThreadLocal<OcrService> tesseractThreadLocal;

    public OcrServiceProvider(TsvParser tsvParser, PathsUtils pathsUtils) {
        tesseractThreadLocal = ThreadLocal
                .withInitial(() -> {
                    log.info("New Tesseract created");
                    return new OcrService(pathsUtils, tsvParser);
                });
    }

    public OcrService get() {
        return tesseractThreadLocal.get();
    }
}
