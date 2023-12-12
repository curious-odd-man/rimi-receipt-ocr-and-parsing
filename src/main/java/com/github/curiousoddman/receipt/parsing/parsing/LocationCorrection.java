package com.github.curiousoddman.receipt.parsing.parsing;

import lombok.RequiredArgsConstructor;

import java.awt.*;

@RequiredArgsConstructor
public class LocationCorrection {
    private final int xCorrection;
    private final int widthCorrection;

    public static final LocationCorrection NO_CORRECTION = new LocationCorrection(0, 0) {
        @Override
        public Rectangle getCorrectedLocation(Rectangle rectangle) {
            return rectangle;
        }
    };

    public static LocationCorrection xCorrection(int x) {
        return new LocationCorrection(x, 0);
    }

    public static LocationCorrection widthCorrection(int width) {
        return new LocationCorrection(0, width);
    }

    public static LocationCorrection correction(int x, int width) {
        return new LocationCorrection(x, width);
    }

    public Rectangle getCorrectedLocation(Rectangle rectangle) {
        int correctedX = rectangle.x + xCorrection;
        int correctedWidth = rectangle.width + widthCorrection;

        return new Rectangle(correctedX, rectangle.y, correctedWidth, rectangle.height);
    }
}
