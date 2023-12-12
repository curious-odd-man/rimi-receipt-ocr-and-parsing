package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface Positioned {
    int getX();

    int getY();

    int getWidth();

    int getHeight();

    @JsonIgnore
    int getIndex();

    @JsonIgnore
    default int getEndX() {
        return getX() + getWidth();
    }

    @JsonIgnore
    default int getEndY() {
        return getY() + getHeight();
    }
}
