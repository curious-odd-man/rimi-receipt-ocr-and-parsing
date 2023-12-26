package com.github.curiousoddman.receipt.alt.main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.curiousoddman.receipt.parsing.stats.AllNumberCollector;
import com.github.curiousoddman.receipt.parsing.stats.MyRect;
import com.github.curiousoddman.receipt.parsing.stats.NumberOnReceipt;
import com.github.curiousoddman.receipt.parsing.utils.JsonUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class NumberImageStatisticsProcessor {
    private static final Path       ROOT_DIR                                    = AllNumberCollector.ALL_NUMBER_WORDS_JSON.getParent();
    private static final Path       EXTRACTED_LETTER_FILE                       = ROOT_DIR.resolve("extracted-letters.json");
    private static final int        N_THREADS                                   = 7;
    private static final ImageCache IMAGE_CACHE                                 = new ImageCache();
    private static final boolean    PRINT_ALL_RECEIPTS_WITH_RECT_AROUND_NUMBERS = true;
    private static final boolean    SAVE_CATEGORIZED_IMAGES                     = true;
    private static final boolean    SAVE_CATEGORIZED_IMAGES_SELECTED            = true;

    public static void main(String[] args) {
        NumberOnReceipt[] numberOnReceipts = loadInputFile();

        Map<FontLetter, List<ImgLetter>> groupedByLetter = readOrExtractLetterImages(numberOnReceipts);
        Map<FontLetter, Map<Size, List<ImgLetter>>> imageLettersGroupBySizes = groupEachLetterBySizes(groupedByLetter);
        Map<FontLetter, List<ImgLetter>> imageLettersWithLargestMatchingSize = eachLetterSameSizeLargestGroup(imageLettersGroupBySizes);
    }

    @SneakyThrows
    private static Map<FontLetter, List<ImgLetter>> readOrExtractLetterImages(NumberOnReceipt[] numberOnReceipts) {
        if (Files.exists(EXTRACTED_LETTER_FILE)) {
            String fileContents = Files.readString(EXTRACTED_LETTER_FILE);
            TypeReference<Map<FontLetter, List<ImgLetter>>> typeRef = new TypeReference<>() {
            };

            return JsonUtils.OBJECT_MAPPER.readValue(fileContents, typeRef);
        }

        Map<FontLetter, List<ImgLetter>> groupedByLetter = getGroupedByLetter(numberOnReceipts);

        String json = JsonUtils.OBJECT_WRITER.writeValueAsString(groupedByLetter);
        Files.writeString(EXTRACTED_LETTER_FILE, json, StandardOpenOption.CREATE_NEW);
        return groupedByLetter;
    }

    private static Map<FontLetter, List<ImgLetter>> eachLetterSameSizeLargestGroup(Map<FontLetter, Map<Size, List<ImgLetter>>> imageLettersGroupBySizes) {
        Map<FontLetter, List<ImgLetter>> imageLettersWithLargestMatchingSize = new HashMap<>();

        log.info("Letter size statistics:");

        for (Map.Entry<FontLetter, Map<Size, List<ImgLetter>>> entry : imageLettersGroupBySizes.entrySet()) {
            FontLetter letter = entry.getKey();
            Map<Size, List<ImgLetter>> sizes = entry.getValue();
            List<ImgLetter> largest = null;
            log.info("{}", letter);
            for (Map.Entry<Size, List<ImgLetter>> sizeAndLetter : sizes.entrySet()) {
                log.info("\t{} - {}", sizeAndLetter.getKey(), sizeAndLetter.getValue().size());
                if (largest == null) {
                    largest = sizeAndLetter.getValue();
                } else if (largest.size() < sizeAndLetter.getValue().size()) {
                    largest = sizeAndLetter.getValue();
                }
            }
            imageLettersWithLargestMatchingSize.put(letter, largest);
        }

        if (SAVE_CATEGORIZED_IMAGES_SELECTED) {
            saveCategorizedLettersWithLargestSameSize(imageLettersWithLargestMatchingSize);
        }

        return imageLettersWithLargestMatchingSize;
    }

    private static Map<FontLetter, Map<Size, List<ImgLetter>>> groupEachLetterBySizes(Map<FontLetter, List<ImgLetter>> groupedByLetter) {
        Map<FontLetter, Map<Size, List<ImgLetter>>> imageLettersGroupBySizes = new HashMap<>();

        for (Map.Entry<FontLetter, List<ImgLetter>> entry : groupedByLetter.entrySet()) {
            FontLetter letter = entry.getKey();
            List<ImgLetter> listOfImages = entry.getValue();
            for (ImgLetter img : listOfImages) {
                imageLettersGroupBySizes
                        .computeIfAbsent(letter, k -> new HashMap<>())
                        .computeIfAbsent(new Size(img.w(), img.h()), k -> new ArrayList<>())
                        .add(img);
            }
        }
        return imageLettersGroupBySizes;
    }

    @SneakyThrows
    private static Map<FontLetter, List<ImgLetter>> getGroupedByLetter(NumberOnReceipt[] numberOnReceipts) {
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        Map<Path, List<NumberOnReceipt>> groupedByFile = Arrays.stream(numberOnReceipts).collect(Collectors.groupingBy(NumberOnReceipt::filePath));
        Iterator<List<NumberOnReceipt>> iterator = groupedByFile.values().iterator();

        Map<FontLetter, List<ImgLetter>> groupedByLetter = new HashMap<>();

        while (iterator.hasNext()) {
            List<NumberOnReceipt> next = iterator.next();
            for (NumberOnReceipt numberOnReceipt : next) {
                Path imageFile = numberOnReceipt.filePath();
                String numberText = numberOnReceipt.numberText();
                executorService.submit(() -> extractNumberFromFile(numberOnReceipt, imageFile, numberText, groupedByLetter));
            }
        }

        if (PRINT_ALL_RECEIPTS_WITH_RECT_AROUND_NUMBERS) {
            debugPrintAllReceipts(groupedByFile);
        }

        log.info("Awaiting completion of threads....");
        executorService.shutdown();
        while (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
            log.info("Still waiting...");
        }
        return groupedByLetter;
    }

    @SneakyThrows
    private static NumberOnReceipt[] loadInputFile() {
        String inputJsonContents = Files.readString(AllNumberCollector.ALL_NUMBER_WORDS_JSON);
        return JsonUtils
                .OBJECT_MAPPER
                .readValue(inputJsonContents, NumberOnReceipt[].class);
    }

    private static void extractNumberFromFile(NumberOnReceipt numberOnReceipt,
                                              Path imageFile,
                                              String numberText,
                                              Map<FontLetter, List<ImgLetter>> groupedByLetter) {
        List<ImgLetter> imgLetterList = splitToLetters(imageFile, numberOnReceipt);
        int symbolCount = numberText.length();
        if (symbolCount != imgLetterList.size()) {
            log.info("Count of letters in image and in text does not match: {} : {}", imageFile, numberText);
            return;
        }

        for (int i = 0; i < numberText.length(); i++) {
            ImgLetter imgLetter = imgLetterList.get(i);
            FontLetter currentLetter = new FontLetter(numberOnReceipt.type(), numberText.substring(i, i + 1));
            if (SAVE_CATEGORIZED_IMAGES) {
                saveCategorizedLetterImage(currentLetter, imageFile, imgLetter);
            }
            groupedByLetter
                    .computeIfAbsent(currentLetter, k -> new ArrayList<>())
                    .add(imgLetter);
        }
    }

    private static void saveCategorizedLettersWithLargestSameSize(Map<FontLetter, List<ImgLetter>> imageLettersWithLargestMatchingSize) {
        Path parentDir = ROOT_DIR
                .resolve("numbers")
                .resolve("categorised-selected");

        for (Map.Entry<FontLetter, List<ImgLetter>> entry : imageLettersWithLargestMatchingSize.entrySet()) {
            FontLetter currentLetter = entry.getKey();
            List<ImgLetter> images = entry.getValue();
            for (ImgLetter image : images) {
                Path fileDirectory = parentDir
                        .resolve(currentLetter.getFont())
                        .resolve(currentLetter.getLetter());
                Path filePath = fileDirectory
                        .resolve(String.format("%s_x%d_y%d_xx%d_yy%d", image.filePath().getFileName(), image.x(), image.y(), image.endX(), image.endY()) + ".png");
                createAllDirectories(fileDirectory);
                IMAGE_CACHE.saveImgLetter(image, filePath);
            }
        }
    }

    private static void saveCategorizedLetterImage(FontLetter currentLetter, Path imageFile, ImgLetter imgLetter) {
        Path parentDir = ROOT_DIR
                .resolve("numbers")
                .resolve("categorised")
                .resolve(currentLetter.getFont())
                .resolve(currentLetter.getLetter());
        Path filePath = parentDir
                .resolve(String.format("%s_x%d_y%d_xx%d_yy%d", imageFile.getFileName(), imgLetter.x(), imgLetter.y(), imgLetter.endX(), imgLetter.endY()) + ".png");

        createAllDirectories(parentDir);
        IMAGE_CACHE.saveImgLetter(imgLetter, filePath);
    }

    @SneakyThrows
    private static void createAllDirectories(Path parentDir) {
        Files.createDirectories(parentDir);
    }

    @SneakyThrows
    private static void debugPrintAllReceipts(Map<Path, List<NumberOnReceipt>> groupedByFile) {
        for (Map.Entry<Path, List<NumberOnReceipt>> entry : groupedByFile.entrySet()) {
            Path file = entry.getKey();
            List<NumberOnReceipt> items = entry.getValue();
            BufferedImage image = copyImage(IMAGE_CACHE.loadImage(file));
            Graphics graphics = image.getGraphics();
            graphics.setColor(Color.BLACK);
            for (NumberOnReceipt item : items) {
                MyRect location = item.location();
                graphics.drawRect(location.x(), location.y(), location.w(), location.h());
            }
            String fileName = file.getFileName().toString();
            File output = ROOT_DIR.resolve("numbers").resolve(fileName + ".markup.png").toFile();
            graphics.dispose();
            ImageIO.write(image, "png", output);
        }
    }

    public static BufferedImage copyImage(BufferedImage source) {
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    @SneakyThrows
    private static List<ImgLetter> splitToLetters(Path imageFile, NumberOnReceipt num) {
        MyRect textLocationInFile = num.location();
        List<ImgLetter> result = new ArrayList<>();
        BufferedImage image = IMAGE_CACHE.loadImage(imageFile);
        int minX = textLocationInFile.x();
        int maxX = minX + textLocationInFile.w();
        int minY = textLocationInFile.y();
        int maxY = minY + textLocationInFile.h();

        int imgStartX = Integer.MAX_VALUE;
        int imgStartY = Integer.MAX_VALUE;
        int imgEndY = -1;
        boolean foundBlackDot = false;

        for (int x = minX; x <= maxX; x++) {
            if (isColumnWhite(image, x, minY, maxY)) {
                if (foundBlackDot) {
                    result.add(new ImgLetter(imgStartX, imgStartY, x - 1, imgEndY, num.filePath()));
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
                        imgStartY = Integer.min(imgStartY, y);
                        imgEndY = Integer.max(imgEndY, y);
                    }
                }
            }
        }

        return result;
    }

    private static boolean isColumnWhite(BufferedImage image, int x, int minY, int maxY) {
        for (int i = minY; i <= maxY; i++) {
            if (isBlackPixel(image, x, i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlackPixel(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0x00FFFFFF) == 0;
    }
}
