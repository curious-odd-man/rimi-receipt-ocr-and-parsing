package com.github.curiousoddman.receipt.parsing.characters;


import com.github.curiousoddman.receipt.alt.main.img.ImgMatrix;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

@Slf4j
@UtilityClass
public class CharImages {
    private static final Path ROOT_DIR = Path.of("src/main/resources/font-images");

    public static final CharImage[] LARGE_FONT = {
            create("L", "0"),
            create("L", "1"),
            create("L", "2"),
            create("L", "3"),
            create("L", "4"),
            create("L", "5"),
            create("L", "6"),
            create("L", "7"),
            create("L", "8"),
            create("L", "9"),
            create("L", "comma")
    };

    public static final CharImage[] SMALL_FONT = {
            create("S", "0"),
            create("S", "1"),
            create("S", "2"),
            create("S", "3"),
            create("S", "4"),
            create("S", "5"),
            create("S", "6"),
            create("S", "7"),
            create("S", "8"),
            create("S", "9"),
            create("S", "comma"),
            create("S", "dash")
    };

    public static final CharImage[] EXTRA_LARGE_FONT = {
            create("XL", "0"),
            create("XL", "1"),
            create("XL", "2"),
            create("XL", "3"),
            create("XL", "4"),
            create("XL", "5"),
            create("XL", "6"),
            create("XL", "7"),
            create("XL", "8"),
            create("XL", "9"),
            create("XL", "comma")
    };

    @SneakyThrows
    private static CharImage create(String font, String symbol) {
        Path filePath = ROOT_DIR.resolve(font).resolve(symbol + ".png");
        log.info("Loading image file: {}", filePath.toAbsolutePath());
        BufferedImage bufferedImage = ImageIO.read(filePath.toFile());
        ImgMatrix image = new ImgMatrix(bufferedImage);
        return new CharImage(font, symbol, image);
    }
}
