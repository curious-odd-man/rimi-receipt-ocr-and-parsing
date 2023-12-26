package com.github.curiousoddman.receipt.alt.main;

import java.nio.file.Path;

public record ImgLetter(int x, int y, int endX, int endY, Path filePath) {
    public int w() {
        return endX - x;
    }

    public int h() {
        return endY - y;
    }
}
