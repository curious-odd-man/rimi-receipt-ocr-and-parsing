package com.github.curiousoddman.receipt.parsing.ocr;

import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;

import java.nio.file.Path;

public record OcrResult(OriginFile originFile, String plainText, TsvDocument tsvDocument) {
    public Path cacheDir() {
        return originFile.preprocessedTiff().getParent();
    }
}
