package com.github.curiousoddman.receipt.parsing.ocr.tsv.document;

public interface Positioned {
    int getX();

    int getY();

    int getWidth();

    int getHeight();
}
