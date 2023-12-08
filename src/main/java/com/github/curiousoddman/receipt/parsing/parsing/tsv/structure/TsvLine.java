package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class TsvLine {
    private final TsvRow        row;
    private final List<TsvWord> words;

    public int lineNum() {
        return row.lineNum();
    }

    public String getText() {
        return words.stream().map(TsvWord::getText).collect(Collectors.joining(" "));
    }
}
