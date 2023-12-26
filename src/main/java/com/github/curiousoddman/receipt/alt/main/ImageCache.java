package com.github.curiousoddman.receipt.alt.main;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ImageCache {
    private final Map<Path, BufferedImage> images;

    public ImageCache() {
        this(new ConcurrentHashMap<>());
    }

    @SneakyThrows
    public BufferedImage loadImage(Path path) {
        return images.computeIfAbsent(path, ImageCache::readFile);
    }

    @SneakyThrows
    private static BufferedImage readFile(Path path) {
        return ImageIO.read(path.toFile());
    }

    @SneakyThrows
    public void saveImgLetter(ImgLetter imgLetter, Path filePath) {
        BufferedImage subimage = loadImage(imgLetter.filePath())
                .getSubimage(imgLetter.x(), imgLetter.y(), imgLetter.w(), imgLetter.h());
        ImageIO.write(subimage, "png", filePath.toFile());
    }
}
