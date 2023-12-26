package com.github.curiousoddman.receipt.alt.main;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class FontLetter {
    private final String font;
    private final String letter;

    public FontLetter(String json) {
        // FontLetter[font=s, letter=,]
        if (!json.startsWith("FontLetter[")) {
            throw new IllegalArgumentException();
        }

        String fontKey = "font=";
        int fontIndex = json.indexOf(fontKey) + fontKey.length();
        int indexOfCommaAfterFont = json.indexOf(',', fontIndex);
        font = json.substring(fontIndex, indexOfCommaAfterFont);

        String letterKey = "letter=";
        int letterIndex = json.indexOf(letterKey) + letterKey.length();
        letter = json.substring(letterIndex, letterIndex + 1);
    }
}
