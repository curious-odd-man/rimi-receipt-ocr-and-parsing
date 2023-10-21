package com.github.curiousoddman.receipt.parsing.cache;

import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@Slf4j
@Component
public class FileCache {
    private static final Path CACHE_ROOT = Path.of("D:\\Programming\\git\\caches");

    @SneakyThrows
    public MyTessResult getOrCreate(String cacheName, String identifier, Supplier<MyTessResult> valueSupplier) {
        Path currentCacheDir = CACHE_ROOT.resolve(cacheName);
        Path textCacheFilePath = currentCacheDir.resolve(identifier + ".txt");
        Path tsvCacheFilePath = currentCacheDir.resolve(identifier + ".tsv");
        if (Files.exists(textCacheFilePath) && Files.exists(tsvCacheFilePath)) {
            return new MyTessResult(
                    Files.readString(textCacheFilePath),
                    Files.readString(tsvCacheFilePath)
            );
        }

        MyTessResult text = valueSupplier.get();
        Files.createDirectories(currentCacheDir);
        Files.writeString(textCacheFilePath, text.plainText());
        Files.writeString(tsvCacheFilePath, text.tsvText());
        return text;
    }

    @SneakyThrows
    public File getOrCreateFile(String cacheName, String identifier, Supplier<File> valueSupplier) {
        Path currentCacheDir = CACHE_ROOT.resolve(cacheName);
        Path cacheFilePath = currentCacheDir.resolve(identifier);
        if (Files.exists(cacheFilePath)) {
            return cacheFilePath.toFile();
        }

        File createdFile = valueSupplier.get();
        Files.createDirectories(currentCacheDir);
        Files.copy(createdFile.toPath(), cacheFilePath);
        return createdFile;
    }

    @SneakyThrows
    public void create(String cacheName, String identifier, String contents) {
        Path currentCacheDir = CACHE_ROOT.resolve(cacheName);
        Path cacheFilePath = currentCacheDir.resolve(identifier);

        Files.createDirectories(currentCacheDir);
        Files.writeString(cacheFilePath, contents);
    }
}
