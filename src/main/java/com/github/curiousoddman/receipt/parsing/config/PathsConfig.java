package com.github.curiousoddman.receipt.parsing.config;

import java.nio.file.Path;

public class PathsConfig {
    public static final Path CACHES                 = Path.of("W:\\Programming\\git\\caches");
    public static final Path PRIVATE_TOOLS_ROOT     = Path.of("W:\\Programming\\git\\private-tools");
    public static final Path ALL_NUMBER_WORDS_JSON  = CACHES.resolve("all-number-words.json");
    public static final Path PDF_INPUT_DIR          = PRIVATE_TOOLS_ROOT.resolve("gmail-client\\output");
    public static final Path VALIDATION_RESULT_JSON = CACHES.resolve("validation-result.json");
    public static final Path IGNORE_PDF_CONFIG_PATH = PRIVATE_TOOLS_ROOT.resolve("receipts-parsing\\data\\ignore.txt");
    public static final Path WHITELIST_CONFIG_PATH  = PRIVATE_TOOLS_ROOT.resolve("receipts-parsing\\data\\whitelist.txt");

    public static final String TESSERACT_MODEL_PATH = PRIVATE_TOOLS_ROOT.resolve("receipts-parsing\\tes").toAbsolutePath().toString();
}
