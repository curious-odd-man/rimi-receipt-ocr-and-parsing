package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class TsvLine {
    @JsonIgnore
    private final TsvParagraph parentParagraph;

    private final int lineNum;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<TsvWord> words;

    public String getText() {
        return words.stream().map(TsvWord::getText).collect(Collectors.joining(" "));
    }
}
