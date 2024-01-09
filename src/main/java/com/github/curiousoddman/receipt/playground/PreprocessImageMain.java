package com.github.curiousoddman.receipt.playground;

import com.github.curiousoddman.receipt.parsing.utils.ImageUtils;
import nu.pattern.OpenCV;

import java.nio.file.Path;

public class PreprocessImageMain {
    public static void main(String[] args) {
        OpenCV.loadLocally();
        var source = Path.of("W:\\Programming\\git\\caches\\cache\\2-214693\\2-214693.pdf.tiff");
        var target = Path.of("W:\\Programming\\git\\caches\\cache\\2-214693\\2-214693.pdf.preprocessed_test.tiff");
        ImageUtils.doImagePreprocessing(source, target);
    }
}
