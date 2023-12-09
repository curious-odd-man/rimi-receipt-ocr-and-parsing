package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class TsvLine {
    @JsonIgnore
    private final TsvParagraph parentParagraph;

    private final int lineNum;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<TsvWord> words;

    public String getText() {
        return words.stream().map(TsvWord::getText).collect(Collectors.joining(" "));
    }

    public boolean contains(String text) {
        return getText().contains(text);
    }

    public static TsvLine dummy(String text) {
        return new TsvLine(null, -1, 0, 0, 0, 0, new ArrayList<>()) {
            @Override
            public String getText() {
                return text;
            }
        };
    }

    @JsonIgnore
    public boolean isBlank() {
        return getText().isBlank();
    }

    public Optional<TsvWord> getWordByWordNum(int wordNum) {
        return words
                .stream()
                .filter(w -> w.getWordNum() == wordNum)
                .findAny();
    }
}
