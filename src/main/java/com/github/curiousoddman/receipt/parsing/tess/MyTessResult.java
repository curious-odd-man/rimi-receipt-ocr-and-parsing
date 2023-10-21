package com.github.curiousoddman.receipt.parsing.tess;

import java.io.File;

public record MyTessResult(File inputFile, String plainText, String tsvText) {

}
