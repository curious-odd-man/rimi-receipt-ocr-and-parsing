package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class OcrResultPage {
    @JsonIgnore
    private final OcrTsvResult parentDocument;
    private final int          pageNum;
    private final int         x;
    private final int         y;
    private final int         width;
    private final int         height;

    private final List<OcrResultBlock> blocks;

    @Override
    public String toString() {
        return blocks.stream().map(OcrResultBlock::toString).collect(Collectors.joining(";"));
    }
}
