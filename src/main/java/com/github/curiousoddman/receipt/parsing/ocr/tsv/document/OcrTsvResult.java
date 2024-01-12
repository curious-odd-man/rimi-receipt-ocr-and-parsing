package com.github.curiousoddman.receipt.parsing.ocr.tsv.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class OcrTsvResult {
    @JsonIgnore
    private final String              tsvFileContents;
    private final List<OcrResultPage> pages;

    @JsonIgnore
    public List<OcrResultLine> getLines() {
        return pages
                .stream()
                .map(OcrResultPage::getBlocks)
                .flatMap(Collection::stream)
                .map(OcrResultBlock::getParagraphs)
                .flatMap(Collection::stream)
                .map(OcrResultParagraph::getLines)
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public String toString() {
        return pages.stream().map(OcrResultPage::toString).collect(Collectors.joining(";"));
    }
}
