package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TsvLine {
    private final TsvRow        row;
    private final List<TsvWord> words;

    public int lineNum() {
        return row.lineNum();
    }
}
