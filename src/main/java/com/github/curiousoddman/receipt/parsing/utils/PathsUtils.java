package com.github.curiousoddman.receipt.parsing.utils;

import com.github.curiousoddman.receipt.parsing.config.PathsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class PathsUtils {
    private final PathsConfig pathsConfig;

    public Path getCachesRoot() {
        return Path.of(pathsConfig.getCachesRoot());
    }

    public Path getPdfInputDir() {
        return Path.of(pathsConfig.getInputDir());
    }

    public Path getValidationResultPath() {
        return getCachesRoot().resolve("validation-result.json");
    }

    public Path getIgnoreFilePath() {
        return Path.of("ignore.txt");
    }

    public Path getWhitelistFilePath() {
        return Path.of("whitelist.txt");
    }

    public String getTesseractModelPath() {
        return Path.of("tes").toAbsolutePath().toString();
    }

    public static boolean isPdfFile(Path file) {
        return file.toFile().getName().endsWith(".pdf");
    }
}
