package com.github.curiousoddman.receipt.parsing.tess;

import java.awt.*;

public record MyTessWord(int level,
                         int pageNum,
                         int blockNum,
                         int paragraphNum,
                         int lineNum,
                         int wordNum,
                         int left,
                         int top,
                         int width,
                         int height,
                         double conf,
                         String text) {

    public Rectangle getWordRect() {
        return new Rectangle(left() - 2, top() - 2, width() + 2, height() + 2);
    }

    public boolean isFollowedBy(MyTessWord other) {
        return pageNum == other.pageNum
                && blockNum == other.blockNum
                && paragraphNum == other.paragraphNum
                && lineNum == other.lineNum
                && wordNum + 1 == other.wordNum;
    }
}
