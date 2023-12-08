package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class TsvContainerKey {
    int page;
    int block;
    int paragraph;
}
