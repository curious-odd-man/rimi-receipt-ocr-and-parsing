package com.github.curiousoddman.receipt.parsing.cache;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@Slf4j
@Component
public class FileCache {
    private static final Path CACHE_ROOT = Path.of("D:\\Programming\\git\\caches");

    @SneakyThrows
    public String getOrCreate(String cacheName, String identifier, Supplier<String> valueSupplier) {
        Path currentCacheDir = CACHE_ROOT.resolve(cacheName);
        Path cacheFilePath = currentCacheDir.resolve(identifier);
        if (Files.exists(cacheFilePath)) {
            return Files.readString(cacheFilePath);
        }

        String text = valueSupplier.get();
        Files.createDirectories(currentCacheDir);
        Files.writeString(cacheFilePath, text);
        return text;
    }

    @SneakyThrows
    public <T> T getOrCreate(String cacheName, String identifier, Supplier<String> valueSupplier, ThrowingFunction<String, T, ? extends Exception> convertToType) {
        String text = getOrCreate(cacheName, identifier, valueSupplier);
        return convertToType.apply(text);
    }

    @SneakyThrows
    public void create(String cacheName, String identifier, String contents) {
        Path currentCacheDir = CACHE_ROOT.resolve(cacheName);
        Path cacheFilePath = currentCacheDir.resolve(identifier);

        Files.createDirectories(currentCacheDir);
        Files.writeString(cacheFilePath, contents);
    }
}
