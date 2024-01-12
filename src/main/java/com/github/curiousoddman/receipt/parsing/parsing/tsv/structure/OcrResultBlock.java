package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class OcrResultBlock {
    @JsonIgnore
    private final OcrResultPage parentPage;

    private final int blockNum;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<OcrResultParagraph> paragraphs;


    @Override
    public String toString() {
        return paragraphs.stream().map(OcrResultParagraph::toString).collect(Collectors.joining(";"));
    }
}
