package com.github.curiousoddman.receipt.parsing.stats;

import java.awt.*;

public record MyRect(int x, int y, int w, int h) {
    public static MyRect of(Rectangle r) {
        return new MyRect(r.x, r.y, r.width, r.height);
    }
}
