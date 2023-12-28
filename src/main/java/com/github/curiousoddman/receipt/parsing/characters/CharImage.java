package com.github.curiousoddman.receipt.parsing.characters;

import com.github.curiousoddman.receipt.alt.main.img.ImgMatrix;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CharImage {
    private final String    font;
    private final String    character;
    private final ImgMatrix image;

    @Override
    public String toString() {
        return font + '_' + character;
    }
}
