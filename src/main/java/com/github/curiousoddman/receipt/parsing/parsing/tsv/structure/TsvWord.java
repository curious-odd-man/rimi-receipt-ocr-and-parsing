package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class TsvWord {
    private final TsvRow row;

    @JsonIgnore
    public int getWordNumber() {
        return row.wordNum();
    }

    public int getX() {
        return row.left();
    }

    public int getY() {
        return row.top();
    }

    public int getWidth() {
        return row.width();
    }

    public int getHeight() {
        return row.height();
    }

    public BigDecimal getConfidence() {
        return row.confidence();
    }

    public String getText() {
        return row.text();
    }
}
