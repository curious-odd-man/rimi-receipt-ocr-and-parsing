package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TsvDocument {
    private final List<TsvContainer> containerList;
}
