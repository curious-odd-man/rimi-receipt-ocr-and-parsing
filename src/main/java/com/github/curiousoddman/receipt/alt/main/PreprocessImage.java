package com.github.curiousoddman.receipt.alt.main;

import com.github.curiousoddman.receipt.parsing.utils.ImageUtils;
import nu.pattern.OpenCV;

import java.nio.file.Path;

public class PreprocessImage {
    public static void main(String[] args) {
        OpenCV.loadLocally();
        var source = Path.of("W:\\Programming\\git\\caches\\cache\\2-214693\\2-214693.pdf.tiff");
        var target = Path.of("W:\\Programming\\git\\caches\\cache\\2-214693\\2-214693.pdf.preprocessed_test.tiff");
        ImageUtils.doImagePreprocessing(source, target);
    }
}
