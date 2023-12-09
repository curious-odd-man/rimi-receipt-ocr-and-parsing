package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TsvPage {
    @JsonIgnore
    private final TsvDocument parentDocument;
    private final int         pageNum;
    private final int         x;
    private final int         y;
    private final int         width;
    private final int         height;

    private final List<TsvBlock> blocks;
}
