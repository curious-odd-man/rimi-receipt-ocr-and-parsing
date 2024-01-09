package com.github.curiousoddman.receipt.parsing.ocr;

import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class OcrResult {
    private final OriginFile originFile;
    private final String     plainText;
    private final String     tsvText;

    private TsvDocument tsvDocument;
}
