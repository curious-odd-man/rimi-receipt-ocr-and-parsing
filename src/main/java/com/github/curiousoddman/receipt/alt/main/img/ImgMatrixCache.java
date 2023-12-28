package com.github.curiousoddman.receipt.alt.main.img;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ImgMatrixCache {
    private final Map<Path, ImgMatrix> images;

    public ImgMatrixCache() {
        this(new ConcurrentHashMap<>());
    }

    @SneakyThrows
    public ImgMatrix loadImage(Path path) {
        //return images.computeIfAbsent(path, ImgMatrixCache::readFile);
        return readFile(path);
    }

    @SneakyThrows
    private static ImgMatrix readFile(Path path) {
        return new ImgMatrix(ImageIO.read(path.toFile()));
    }
}
