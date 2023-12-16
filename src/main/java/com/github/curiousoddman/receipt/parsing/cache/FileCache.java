package com.github.curiousoddman.receipt.parsing.cache;

import com.github.curiousoddman.receipt.parsing.opencv.OpenCvUtils;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.OcrConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static com.github.curiousoddman.receipt.parsing.tess.MyTesseract.getImageFile;

@Slf4j
@Component
public class FileCache {
    private static final Path CACHE_ROOT = Path.of("D:\\Programming\\git\\caches\\cache");

    @SneakyThrows
    public FileCache() {
        Files.createDirectories(CACHE_ROOT);
    }

    @SneakyThrows
    public MyTessResult getOrCreate(Path pdfFile, Function<OcrConfig, MyTessResult> valueSupplier) {
        Path pdfFileName = pdfFile.getFileName();
        Path newRoot = getSubdirectoryPath(pdfFile);
        Path textCacheFilePath = newRoot.resolve(pdfFileName + ".txt");
        Path tsvCacheFilePath = newRoot.resolve(pdfFileName + ".tsv");
        Path imageCacheFilePath = newRoot.resolve(pdfFileName + ".tiff");
        if (Files.exists(textCacheFilePath) && Files.exists(tsvCacheFilePath)) {
            return new MyTessResult(
                    pdfFile,
                    imageCacheFilePath,
                    Files.readString(textCacheFilePath),
                    Files.readString(tsvCacheFilePath)
            );
        }

        if (!Files.exists(imageCacheFilePath)) {
            File imageFile = getImageFile(pdfFile.toFile());
            Files.copy(imageFile.toPath(), imageCacheFilePath);
        }

        Path preprocessedImagePath = newRoot.resolve(pdfFileName + ".preprocessed.tiff");

        if (!Files.exists(preprocessedImagePath)) {
            OpenCvUtils.doImagePreprocessing(imageCacheFilePath, preprocessedImagePath);
        }

        MyTessResult tessResult = valueSupplier.apply(OcrConfig.builder(pdfFile, preprocessedImagePath).build());
        Files.writeString(textCacheFilePath, tessResult.getPlainText());
        Files.writeString(tsvCacheFilePath, tessResult.getTsvText());
        return tessResult;
    }

    @SneakyThrows
    private static Path getSubdirectoryPath(Path pdfFile) {
        String fileName = pdfFile.toFile().getName();
        int i = fileName.indexOf('.');
        String dirName = fileName.substring(0, i);
        Path newRoot = CACHE_ROOT.resolve(dirName);
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
