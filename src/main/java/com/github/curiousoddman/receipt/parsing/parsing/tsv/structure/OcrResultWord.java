package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.awt.*;
import java.math.BigDecimal;

@ToString
@Getter
@RequiredArgsConstructor
public class OcrResultWord implements Positioned {
    @JsonIgnore
    private final OcrResultLine parentLine;

    private final int        wordNum;
    private final int        x;
    private final int        y;
    private final int        width;
    private final int        height;
    private final BigDecimal confidence;
    private final String     text;

    @JsonIgnore
    public Rectangle getWordRect() {
        return new Rectangle(
                x - 2,
                y - 2,
                width + 4,
                height + 4
        );
    }
}
