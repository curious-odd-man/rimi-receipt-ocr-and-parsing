package com.github.curiousoddman.receipt.parsing.stats;

import java.nio.file.Path;

public record NumberOnReceipt(Path filePath, String numberText, MyRect location) {

}
