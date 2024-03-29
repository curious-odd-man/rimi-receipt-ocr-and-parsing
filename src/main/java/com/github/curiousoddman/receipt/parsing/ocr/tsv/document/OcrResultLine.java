package com.github.curiousoddman.receipt.parsing.ocr.tsv.document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class OcrResultLine implements Positioned {
    @JsonIgnore
    private final OcrResultParagraph parentParagraph;

    private final int lineNum;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final List<OcrResultWord> words;

    public String getText() {
        return words.stream().map(OcrResultWord::getText).collect(Collectors.joining(" "));
    }

    public boolean contains(String text) {
        return getText().contains(text);
    }

    public static OcrResultLine dummy(String text) {
        return new OcrResultLine(null, -1, 0, 0, 0, 0, new ArrayList<>()) {
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

    public Optional<OcrResultWord> getWordByWordNum(int wordNum) {
        return words
                .stream()
                .filter(w -> w.getWordNum() == wordNum)
                .findAny();
    }

    public Optional<OcrResultWord> getWordByIndex(int index) {
        try {
            if (index > 0) {
                return Optional.of(words.get(index));
            } else {
                return Optional.of(words.get(words.size() + index));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @JsonIgnore
    public Rectangle getRectangle() {
        return new Rectangle(x - 2,
                             y - 2,
                             width + 4,
                             height + 4);
    }

    @Override
    public String toString() {
        return getText();
    }
}
