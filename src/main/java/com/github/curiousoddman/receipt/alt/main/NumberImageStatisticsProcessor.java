package com.github.curiousoddman.receipt.alt.main;

import com.github.curiousoddman.receipt.parsing.stats.AllNumberCollector;
import com.github.curiousoddman.receipt.parsing.utils.JsonUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class NumberImageStatisticsProcessor {

    private static final Map<Integer, List<ImgLetter>> ALL_IMG_LETTERS = new HashMap<>();

    private static int imgCounter = 0;

    @SneakyThrows
    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(7);
        String contents = Files.readString(AllNumberCollector.OUTPUT_PATH);

        AllNumberCollector.Number[] numbers = JsonUtils.OBJECT_MAPPER.readValue(contents, AllNumberCollector.Number[].class);

        Map<Path, List<AllNumberCollector.Number>> groupedByFile = Arrays.stream(numbers).collect(Collectors.groupingBy(AllNumberCollector.Number::filePath));
        Iterator<List<AllNumberCollector.Number>> iterator = groupedByFile.values().iterator();

        for (int i = 0; iterator.hasNext(); i++) {
            List<AllNumberCollector.Number> next = iterator.next();
            for (AllNumberCollector.Number number : next) {
                Path imageFile = number.filePath();
                String numberText = number.numberText();
                executorService.submit(() -> {
                    List<ImgLetter> imgLetterList = findLettersOnImage(imageFile, number);
                });
            }
        }

        groupedByFile.forEach((file, items) -> {
            try {
                BufferedImage image = ImageIO.read(file.toFile());
                Graphics graphics = image.getGraphics();
                graphics.setColor(Color.BLACK);
                for (AllNumberCollector.Number item : items) {
                    AllNumberCollector.MyRect location = item.location();
                    graphics.drawRect(location.x(), location.y(), location.w(), location.h());
                }
                Path rootDir = AllNumberCollector.OUTPUT_PATH.getParent();
                String fileName = file.getFileName().toString();
                File output = rootDir.resolve("numbers").resolve(fileName + "markeup.png").toFile();
                ImageIO.write(image, "png", output);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
    }

    @SneakyThrows
    private static List<ImgLetter> findLettersOnImage(Path imageFile, AllNumberCollector.Number num) {
        AllNumberCollector.MyRect textLocationInFile = num.location();
        List<ImgLetter> result = new ArrayList<>();
        BufferedImage image = ImageIO.read(imageFile.toFile());
        showImage(image);
        int minX = textLocationInFile.x();
        int maxX = minX + textLocationInFile.w();
        int minY = textLocationInFile.y();
        int maxY = minY + textLocationInFile.h();

        showImage(image.getSubimage(minX, minY, textLocationInFile.w(), textLocationInFile.h()));

        int imgStartX = Integer.MAX_VALUE;
        int imgStartY = Integer.MAX_VALUE;
        int imgEndY = -1;
        boolean foundBlackDot = false;

        for (int x = minX; x <= maxX; x++) {
            if (isColumnWhite(image, x, minY, maxY)) {
                if (foundBlackDot) {
                    imgCounter++;
                    //result.add(new ImgLetter(image, imgStartX, imgStartY, x - 1, imgEndY));
                    BufferedImage subimage = image.getSubimage(imgStartX, imgStartY, (x - 1 - imgStartX), imgEndY - imgStartY);
                    showImage(subimage);

                    Path rootDir = AllNumberCollector.OUTPUT_PATH.getParent();

                    String fileName = imageFile.getFileName().toString();

                    File output = rootDir
                            .resolve("numbers")
                            .resolve(num.type())
                            .resolve(String.format("%s_x%d_y%d_xx%d_yy%d", fileName, imgStartX, imgStartY, x - 1, imgEndY) + ".png")
                            .toFile();
                    //log.info(output.toPath().toAbsolutePath().toString());
                    ImageIO.write(subimage, "png", output);
                }
                foundBlackDot = false;
            } else {
                if (!foundBlackDot) {
                    imgStartX = x;
                    imgStartY = Integer.MAX_VALUE;
                    imgEndY = -1;
                }
                foundBlackDot = true;
                for (int y = minY; y <= maxY; y++) {
                    if (isBlackPixel(image, x, y)) {
                        imgStartX = Integer.min(imgStartX, x);
                        imgStartY = Integer.min(imgStartY, y);
                        imgEndY = Integer.max(imgEndY, y);
                    }
                }
            }
        }

        return result;
    }

    private static void showImage(BufferedImage subimage) {
//        JLabel picLabel = new JLabel(new ImageIcon(subimage));
//
//        JPanel jPanel = new JPanel();
//        jPanel.add(picLabel);
//
//        JFrame f = new JFrame();
//        f.setSize(new Dimension(subimage.getWidth(), subimage.getHeight()));
//        f.add(jPanel);
//        f.setVisible(true);
    }

    private static boolean isColumnWhite(BufferedImage image, int x, int minY, int maxY) {
        for (int i = minY; i <= maxY; i++) {
            if (isBlackPixel(image, x, i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlackPixel(BufferedImage image, int x, int i) {
        return (image.getRGB(x, i) & 0x00FFFFFF) == 0;
    }

    private record ImgLetter(BufferedImage image, int x, int y, int endX, int endY) {

    }
}
