package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TsvContainer {
    @JsonIgnore
    private final TsvContainerKey tsvContainerKey;
    private final TsvRow          pageRow;
    private final TsvRow          blockRow;
    private final TsvRow          paragraphRow;
    private final List<TsvLine>   lines;
}
