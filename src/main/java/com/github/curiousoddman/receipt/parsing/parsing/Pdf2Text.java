package com.github.curiousoddman.receipt.parsing.parsing;

import com.github.curiousoddman.receipt.parsing.cache.FileCache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
public class Pdf2Text {
    private final Tesseract tesseract = new Tesseract();
    private final FileCache fileCache;

    public Pdf2Text(FileCache fileCache) {
        tesseract.setDatapath("D:\\Programming\\git\\private-tools\\receipts-parsing\\tes");
        tesseract.setLanguage("lav");
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(1);
        this.fileCache = fileCache;
    }

    @SneakyThrows
    public String convert(Path path) {
        return tesseract.doOCR(path.toFile());
    }
}
