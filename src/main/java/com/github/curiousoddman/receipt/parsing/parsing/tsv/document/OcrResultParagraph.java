package com.github.curiousoddman.receipt.parsing.parsing.tsv.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class OcrResultParagraph {
    @JsonIgnore
    private final OcrResultBlock parentBlock;

    private final int paragraphNum;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<OcrResultLine> lines;


    @Override
    public String toString() {
        return lines.stream().map(OcrResultLine::toString).collect(Collectors.joining(";"));
    }
}
