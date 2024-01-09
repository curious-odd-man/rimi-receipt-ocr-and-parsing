package com.github.curiousoddman.receipt.parsing.ocr;

import com.github.curiousoddman.receipt.parsing.config.PathsConfig;
import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.utils.ImageUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

import static com.github.curiousoddman.receipt.parsing.utils.ImageUtils.getImageFile;

@Slf4j
@Component
public class FileCache {

    public static final Path FILE_CACHE_DIR = PathsConfig.CACHES.resolve("cache");

    @SneakyThrows
    public FileCache() {
        Files.createDirectories(FILE_CACHE_DIR);
    }

    @SneakyThrows
    public OcrResult getOrCreate(Path pdfFile, BiFunction<OcrConfig, OriginFile, OcrResult> valueSupplier) {
        Path pdfFileName = pdfFile.getFileName();
        Path newRoot = getSubdirectoryPath(pdfFile);
        Path textCacheFilePath = newRoot.resolve(pdfFileName + ".txt");
        Path tsvCacheFilePath = newRoot.resolve(pdfFileName + ".tsv");
        Path imageCacheFilePath = newRoot.resolve(pdfFileName + ".tiff");
        Path preprocessedImagePath = newRoot.resolve(pdfFileName + ".preprocessed.tiff");
        OriginFile originFile = new OriginFile(pdfFile, imageCacheFilePath, preprocessedImagePath);
        if (Files.exists(textCacheFilePath) && Files.exists(tsvCacheFilePath)) {
            return new OcrResult(
                    originFile,
                    Files.readString(textCacheFilePath),
                    Files.readString(tsvCacheFilePath)
            );
        }

        if (!Files.exists(imageCacheFilePath)) {
            File imageFile = getImageFile(pdfFile.toFile());
            Files.copy(imageFile.toPath(), imageCacheFilePath);
        }


        if (!Files.exists(preprocessedImagePath)) {
            ImageUtils.doImagePreprocessing(imageCacheFilePath, preprocessedImagePath);
        }

        OcrConfig ocrConfig = OcrConfig.builder(originFile.preprocessedTiff()).build();
        OcrResult tessResult = valueSupplier.apply(ocrConfig, originFile);
        Files.writeString(textCacheFilePath, tessResult.getPlainText());
        Files.writeString(tsvCacheFilePath, tessResult.getTsvText());
        return tessResult;
    }

    @SneakyThrows
    private static Path getSubdirectoryPath(Path pdfFile) {
        String fileName = pdfFile.toFile().getName();
        int i = fileName.indexOf('.');
        String dirName = fileName.substring(0, i);
        Path newRoot = FILE_CACHE_DIR.resolve(dirName);
        if (!Files.exists(newRoot)) {
            Files.createDirectories(newRoot);
        }
        return newRoot;
    }

    @SneakyThrows
    public void create(Path pdfFile, String contents) {
        Path newRoot = getSubdirectoryPath(pdfFile);
        Path cacheFilePath = newRoot.resolve(pdfFile);
        Files.writeString(cacheFilePath, contents);
    }
}
