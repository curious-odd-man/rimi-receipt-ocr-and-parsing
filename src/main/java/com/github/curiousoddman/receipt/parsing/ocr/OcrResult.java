package com.github.curiousoddman.receipt.parsing.ocr;

import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.document.OcrTsvResult;

import java.nio.file.Path;

public record OcrResult(OriginFile originFile, String plainText, OcrTsvResult ocrTsvResult) {
    public Path cacheDir() {
        return originFile.preprocessedTiff().getParent();
    }
}
