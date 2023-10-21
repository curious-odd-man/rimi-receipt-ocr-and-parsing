package com.github.curiousoddman.receipt.parsing.tess;

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
}
