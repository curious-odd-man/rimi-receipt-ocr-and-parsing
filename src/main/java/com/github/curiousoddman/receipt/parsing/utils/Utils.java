package com.github.curiousoddman.receipt.parsing.utils;

import lombok.experimental.UtilityClass;

import java.awt.*;

@UtilityClass
public class Utils {

    public static Rectangle uniteRectangles(Rectangle r1, Rectangle r2) {
        return r1.union(r2);
    }
}
