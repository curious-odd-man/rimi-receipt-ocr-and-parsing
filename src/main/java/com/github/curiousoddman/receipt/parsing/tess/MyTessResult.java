package com.github.curiousoddman.receipt.parsing.tess;

import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.File;

@Data
@RequiredArgsConstructor
public class MyTessResult {
    private final File   inputFile;
    private final String plainText;
    private final String tsvText;

    private TsvDocument tsvDocument;
}
