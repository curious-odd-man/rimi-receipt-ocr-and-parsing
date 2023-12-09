package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class TsvDocument {
    private final List<TsvPage> pages;

    @JsonIgnore
    public List<TsvLine> getLines() {
        return pages
                .stream()
                .map(TsvPage::getBlocks)
                .flatMap(Collection::stream)
                .map(TsvBlock::getParagraphs)
                .flatMap(Collection::stream)
                .map(TsvParagraph::getLines)
                .flatMap(List::stream)
                .toList();
    }

    @JsonIgnore
    public List<TsvWord> getWords() {
        return pages
                .stream()
                .map(TsvPage::getBlocks)
                .flatMap(Collection::stream)
                .map(TsvBlock::getParagraphs)
                .flatMap(Collection::stream)
                .map(TsvParagraph::getLines)
                .flatMap(List::stream)
                .map(TsvLine::getWords)
                .flatMap(Collection::stream)
                .toList();
    }
}
