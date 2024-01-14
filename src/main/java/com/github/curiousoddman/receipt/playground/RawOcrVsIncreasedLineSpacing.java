package com.github.curiousoddman.receipt.playground;

import com.github.curiousoddman.receipt.parsing.config.DebugConfig;
import com.github.curiousoddman.receipt.parsing.config.PathsConfig;
import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.ocr.OcrConfig;
import com.github.curiousoddman.receipt.parsing.ocr.OcrService;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.TsvParser;
import com.github.curiousoddman.receipt.parsing.utils.ImageUtils;
import com.github.curiousoddman.receipt.parsing.utils.PathsUtils;
import lombok.SneakyThrows;
import nu.pattern.OpenCV;

import java.nio.file.Files;
import java.nio.file.Path;

public class RawOcrVsIncreasedLineSpacing {

    @SneakyThrows
    public static void main(String[] args) {
        OpenCV.loadLocally();
        var source = Path.of("W:\\Programming\\git\\caches\\cache\\2-214693\\2-214693.pdf.tiff");
        var targetExtraSpace = Path.of("W:\\Programming\\git\\caches\\cache\\2-214693\\2-214693.pdf.extra-space.tiff");
        var targetNoExtraSpace = Path.of("W:\\Programming\\git\\caches\\cache\\2-214693\\2-214693.pdf.no-extra-space.tiff");
        ImageUtils.doImagePreprocessing(source, targetExtraSpace, 30);
        ImageUtils.doImagePreprocessing(source, targetNoExtraSpace);

        PathsConfig pathsConfig = new PathsConfig();
        pathsConfig.setCachesRoot(Files.createTempDirectory("RawOcrVsIncreasedLineSpacing").toAbsolutePath().toString());
        OcrService ocrService = new OcrService(
                new PathsUtils(pathsConfig),
                new TsvParser(),
                new DebugConfig()
        );

        OriginFile originFile = new OriginFile(source, targetExtraSpace, targetExtraSpace);
        String extraSpace = ocrService.doMyOCR(OcrConfig.builder(targetExtraSpace).build(), originFile).plainText();
        Files.writeString(source.getParent().resolve("extra-space.txt"), extraSpace);

        OriginFile originFile2 = new OriginFile(source, targetNoExtraSpace, targetNoExtraSpace);
        String noExtraSpace = ocrService.doMyOCR(OcrConfig.builder(targetNoExtraSpace).build(), originFile2).plainText();
        Files.writeString(source.getParent().resolve("no-extra-space.txt"), noExtraSpace);
    }
}

