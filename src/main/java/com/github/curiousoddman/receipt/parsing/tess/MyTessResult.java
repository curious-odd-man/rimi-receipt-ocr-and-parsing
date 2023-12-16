package com.github.curiousoddman.receipt.parsing.tess;

import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

@Data
@RequiredArgsConstructor
public class MyTessResult {
    private final Path   inputFile;
    private final Path   tiffFile;
    private final String plainText;
    private final String tsvText;

    private TsvDocument tsvDocument;
}
