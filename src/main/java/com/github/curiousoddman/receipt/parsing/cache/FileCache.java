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
    private static final Path CACHE_ROOT = Path.of("D:\\Programming\\git\\caches\\cache");

    @SneakyThrows
    public FileCache() {
        Files.createDirectories(CACHE_ROOT);
    }

    @SneakyThrows
    public MyTessResult getOrCreate(String identifier, Supplier<MyTessResult> valueSupplier) {
        Path newRoot = getSubdirectoryPath(identifier);
        Path textCacheFilePath = newRoot.resolve(identifier + ".txt");
        Path tsvCacheFilePath = newRoot.resolve(identifier + ".tsv");
        Path imageCacheFilePath = newRoot.resolve(identifier + ".tiff");
        if (Files.exists(textCacheFilePath) && Files.exists(tsvCacheFilePath)) {
            return new MyTessResult(
                    imageCacheFilePath.toFile(),
                    Files.readString(textCacheFilePath),
                    Files.readString(tsvCacheFilePath)
            );
        }

        MyTessResult tessResult = valueSupplier.get();
        Files.writeString(textCacheFilePath, tessResult.getPlainText());
        Files.writeString(tsvCacheFilePath, tessResult.getTsvText());
        return tessResult;
    }

    @SneakyThrows
    private static Path getSubdirectoryPath(String identifier) {
        int i = identifier.indexOf('.');
        String dirName = identifier.substring(0, i);
        Path newRoot = CACHE_ROOT.resolve(dirName);
        if (!Files.exists(newRoot)) {
            Files.createDirectories(newRoot);
        }
        return newRoot;
    }


    @SneakyThrows
    public File getOrCreateFile(String identifier, Supplier<File> valueSupplier) {
        Path newRoot = getSubdirectoryPath(identifier);
        Path cacheFilePath = newRoot.resolve(identifier);
        if (Files.exists(cacheFilePath)) {
            return cacheFilePath.toFile();
        }

        File createdFile = valueSupplier.get();
        Files.copy(createdFile.toPath(), cacheFilePath);
        return createdFile;
    }

    @SneakyThrows
    public void create(String identifier, String contents) {
        Path newRoot = getSubdirectoryPath(identifier);
        Path cacheFilePath = newRoot.resolve(identifier);
        Files.writeString(cacheFilePath, contents);
    }
}
