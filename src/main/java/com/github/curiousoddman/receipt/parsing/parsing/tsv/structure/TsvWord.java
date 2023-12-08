package com.github.curiousoddman.receipt.parsing.parsing.tsv.structure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.awt.*;
import java.math.BigDecimal;

@ToString
@RequiredArgsConstructor
public class TsvWord {
    private final TsvRow row;

    @JsonIgnore
    public int getWordNumber() {
        return row.wordNum();
    }

    public int getX() {
        return row.left();
    }

    public int getY() {
        return row.top();
    }

    public int getWidth() {
        return row.width();
    }

    public int getHeight() {
        return row.height();
    }

    public BigDecimal getConfidence() {
        return row.confidence();
    }

    public String getText() {
        return row.text();
    }

    @JsonIgnore
    public Rectangle getWordRect() {
        return new Rectangle(
                row.left() - 2,
                row.top() - 2,
                row.width() + 4,
                row.height() + 4
        );
    }

    @JsonIgnore
    public boolean isFollowedBy(TsvWord anotherTessWord) {
        return row.pageNum() == anotherTessWord.row.pageNum()
                && row.blockNum() == anotherTessWord.row.blockNum()
                && row.paragraphNum() == anotherTessWord.row.paragraphNum()
                && row.lineNum() == anotherTessWord.row.lineNum()
                && row.wordNum() + 1 == anotherTessWord.row.wordNum();
    }
}
