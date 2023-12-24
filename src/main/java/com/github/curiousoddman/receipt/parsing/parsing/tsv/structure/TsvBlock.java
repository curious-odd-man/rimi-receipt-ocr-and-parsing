package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class TsvBlock {
    @JsonIgnore
    private final TsvPage parentPage;

    private final int blockNum;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<TsvParagraph> paragraphs;


    @Override
    public String toString() {
        return paragraphs.stream().map(TsvParagraph::toString).collect(Collectors.joining(";"));
    }
}
