package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class TsvDocument {
    private final List<TsvContainer> containerList;

    public List<String> getLines() {
        return containerList
                .stream()
                .map(TsvContainer::getLines)
                .flatMap(List::stream)
                .map(TsvLine::getText)
                .toList();
    }

    public List<TsvWord> getWords() {
        return containerList
                .stream()
                .map(TsvContainer::getLines)
                .flatMap(List::stream)
                .map(TsvLine::getWords)
                .flatMap(Collection::stream)
                .toList();
    }
}
