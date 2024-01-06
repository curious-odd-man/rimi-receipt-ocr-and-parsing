package com.github.curiousoddman.receipt.parsing.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TextUtils {
    public double calculateLikeness(String text1, String text2) {
        int maxLength = Math.max(text1.length(), text2.length());
        int matches = 0;
        for (int i = 0; i < Math.min(text1.length(), text2.length()); i++) {
            if (text1.charAt(i) == text2.charAt(i)) {
                ++matches;
            }
        }
        return matches / (double) maxLength;
    }
}
