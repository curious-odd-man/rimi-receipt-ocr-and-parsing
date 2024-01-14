package com.github.curiousoddman.receipt.parsing.ocr;

import com.github.curiousoddman.receipt.parsing.config.DebugConfig;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.TsvParser;
import com.github.curiousoddman.receipt.parsing.utils.PathsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OcrServiceProvider {
    private final ThreadLocal<OcrService> tesseractThreadLocal;
    private final DebugConfig             debugConfig;

    public OcrServiceProvider(TsvParser tsvParser, PathsUtils pathsUtils, DebugConfig debugConfig) {
        this.debugConfig = debugConfig;
        tesseractThreadLocal = ThreadLocal
                .withInitial(() -> {
                    log.info("New Tesseract created");
                    return new OcrService(pathsUtils, tsvParser, debugConfig);
                });
    }

    public OcrService get() {
        return tesseractThreadLocal.get();
    }
}
