package com.github.curiousoddman.receipt.parsing.parsing;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class Pdf2Text {
    private static final Path      CACHE     = Path.of("D:\\Programming\\git\\private-tools\\receipts-parsing\\data\\cache");
    private final        Tesseract tesseract = new Tesseract();

    public Pdf2Text() {
        tesseract.setDatapath("D:\\Programming\\git\\private-tools\\receipts-parsing\\tes");
        tesseract.setLanguage("lav");
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(1);
    }

    @SneakyThrows
    public String getOrConvert(Path path) {
        Path cacheFilePath = CACHE.resolve(path.getFileName() + ".txt");
        if (Files.exists(cacheFilePath)) {
            log.info("For {} found cached value in {}", path, cacheFilePath);
            return Files.readString(cacheFilePath);
        }

        log.info("For {} will save cache into {}", path, cacheFilePath);
        String text = tesseract.doOCR(path.toFile());
        Files.writeString(cacheFilePath, text);
        log.info("Parsing completed");
        return text;
    }
}
