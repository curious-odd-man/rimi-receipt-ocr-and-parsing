package com.github.curiousoddman.receipt.parsing.cache;

import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

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
    public MyTessResult getOrCreate(Path pdfFile, Function<Path, MyTessResult> valueSupplier) {
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

        MyTessResult tessResult = valueSupplier.apply(imageCacheFilePath);
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
    public File getOrCreateFile(Path pdfFile, Supplier<File> valueSupplier) {
        Path newRoot = getSubdirectoryPath(pdfFile);
        Path cacheFilePath = newRoot.resolve(pdfFile);
        if (Files.exists(cacheFilePath)) {
            return cacheFilePath.toFile();
        }

        File createdFile = valueSupplier.get();
        Files.copy(createdFile.toPath(), cacheFilePath);
        return createdFile;
    }

    @SneakyThrows
    public void create(Path pdfFile, String contents) {
        Path newRoot = getSubdirectoryPath(pdfFile);
        Path cacheFilePath = newRoot.resolve(pdfFile);
        Files.writeString(cacheFilePath, contents);
    }
}
