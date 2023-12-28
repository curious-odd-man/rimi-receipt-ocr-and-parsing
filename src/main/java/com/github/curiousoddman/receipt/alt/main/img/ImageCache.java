package com.github.curiousoddman.receipt.alt.main.img;

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
    public void saveImgLetter(LetterInFile letterInFile, Path filePath) {
        BufferedImage subimage = loadImage(letterInFile.filePath())
                .getSubimage(letterInFile.x(), letterInFile.y(), letterInFile.w(), letterInFile.h());
        ImageIO.write(subimage, "png", filePath.toFile());
    }
}
