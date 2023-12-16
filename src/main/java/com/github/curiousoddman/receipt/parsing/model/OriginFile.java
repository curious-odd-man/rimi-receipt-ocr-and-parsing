package com.github.curiousoddman.receipt.parsing.model;

import java.nio.file.Path;

public record OriginFile(Path pdf,
                         Path convertedTiff,
                         Path preprocessedTiff) {

}
