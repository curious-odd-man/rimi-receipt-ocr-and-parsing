package com.github.curiousoddman.receipt.parsing.utils;

import com.github.curiousoddman.receipt.parsing.ocr.OcrService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class PathsUtils {
    @Value("${config.cachesRoot}")

    public static Path getCachesRoot           = Path.of("W:\\Programming\\git\\caches");
    public static Path getPrivateToolsRoot     = Path.of("W:\\Programming\\git\\private-tools");
    public static Path getPdfInputDir          = getPrivateToolsRoot.resolve("gmail-client\\output");
    public static Path getValidationResultPath = getCachesRoot.resolve("validation-result.json");
    public static Path getIgnoreFilePath       = getPrivateToolsRoot.resolve("receipts-parsing\\data\\ignore.txt");
    public static Path getWhitelistFilePath    = getPrivateToolsRoot.resolve("receipts-parsing\\data\\whitelist.txt");
    public static String getTesseractModelPath = getPrivateToolsRoot.resolve("receipts-parsing\\tes").toAbsolutePath().toString();

    @SneakyThrows
    public static Path getSubdirectoryPath(Path pdfFile) {
        String fileName = pdfFile.toFile().getName();
        int i = fileName.indexOf('.');
        String dirName = fileName.substring(0, i);
        Path newRoot = OcrService.FILE_CACHE_DIR.resolve(dirName);
        if (!Files.exists(newRoot)) {
            Files.createDirectories(newRoot);
        }
        return newRoot;
    }

    public static boolean isPdfFile(Path file) {
        return file.toFile().getName().endsWith(".pdf");
    }
}
