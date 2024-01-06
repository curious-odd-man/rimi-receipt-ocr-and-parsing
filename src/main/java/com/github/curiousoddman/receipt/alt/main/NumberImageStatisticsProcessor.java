package com.github.curiousoddman.receipt.alt.main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.curiousoddman.receipt.alt.main.img.FontLetter;
import com.github.curiousoddman.receipt.alt.main.img.ImageCache;
import com.github.curiousoddman.receipt.alt.main.img.LetterInFile;
import com.github.curiousoddman.receipt.parsing.config.PathsConfig;
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
    private static final Path       ROOT_DIR                                    = PathsConfig.ALL_NUMBER_WORDS_JSON.getParent();
    private static final Path       EXTRACTED_LETTER_FILE                       = ROOT_DIR.resolve("extracted-letters.json");
    private static final int        N_THREADS                                   = 7;
    private static final ImageCache IMAGE_CACHE                                 = new ImageCache();
    private static final boolean    PRINT_ALL_RECEIPTS_WITH_RECT_AROUND_NUMBERS = true;
    private static final boolean    SAVE_CATEGORIZED_IMAGES                     = true;
    private static final boolean    SAVE_CATEGORIZED_IMAGES_SELECTED            = true;

    public static void main(String[] args) {
        NumberOnReceipt[] numberOnReceipts = loadInputFile();

        Map<FontLetter, List<LetterInFile>> groupedByLetter = readOrExtractLetterImages(numberOnReceipts);
        Map<FontLetter, Map<Size, List<LetterInFile>>> imageLettersGroupBySizes = groupEachLetterBySizes(groupedByLetter);
        Map<FontLetter, List<LetterInFile>> imageLettersWithLargestMatchingSize = eachLetterSameSizeLargestGroup(imageLettersGroupBySizes);
    }

    @SneakyThrows
    public static Map<FontLetter, List<LetterInFile>> readOrExtractLetterImages(NumberOnReceipt[] numberOnReceipts) {
        if (Files.exists(EXTRACTED_LETTER_FILE)) {
            String fileContents = Files.readString(EXTRACTED_LETTER_FILE);
            TypeReference<Map<FontLetter, List<LetterInFile>>> typeRef = new TypeReference<>() {
            };

            return JsonUtils.OBJECT_MAPPER.readValue(fileContents, typeRef);
        }

        Map<FontLetter, List<LetterInFile>> groupedByLetter = getGroupedByLetter(numberOnReceipts);

        String json = JsonUtils.OBJECT_WRITER.writeValueAsString(groupedByLetter);
        Files.writeString(EXTRACTED_LETTER_FILE, json, StandardOpenOption.CREATE_NEW);
        return groupedByLetter;
    }

    public static Map<FontLetter, List<LetterInFile>> eachLetterSameSizeLargestGroup(Map<FontLetter, Map<Size, List<LetterInFile>>> imageLettersGroupBySizes) {
        Map<FontLetter, List<LetterInFile>> imageLettersWithLargestMatchingSize = new HashMap<>();

        log.info("Letter size statistics:");

        for (Map.Entry<FontLetter, Map<Size, List<LetterInFile>>> entry : imageLettersGroupBySizes.entrySet()) {
            FontLetter letter = entry.getKey();
            Map<Size, List<LetterInFile>> sizes = entry.getValue();
            List<LetterInFile> largest = null;
            log.info("{}", letter);
            for (Map.Entry<Size, List<LetterInFile>> sizeAndLetter : sizes.entrySet()) {
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

    public static Map<FontLetter, Map<Size, List<LetterInFile>>> groupEachLetterBySizes(Map<FontLetter, List<LetterInFile>> groupedByLetter) {
        Map<FontLetter, Map<Size, List<LetterInFile>>> imageLettersGroupBySizes = new HashMap<>();

        for (Map.Entry<FontLetter, List<LetterInFile>> entry : groupedByLetter.entrySet()) {
            FontLetter letter = entry.getKey();
            List<LetterInFile> listOfImages = entry.getValue();
            for (LetterInFile img : listOfImages) {
                imageLettersGroupBySizes
                        .computeIfAbsent(letter, k -> new HashMap<>())
                        .computeIfAbsent(new Size(img.w(), img.h()), k -> new ArrayList<>())
                        .add(img);
            }
        }
        return imageLettersGroupBySizes;
    }

    @SneakyThrows
    private static Map<FontLetter, List<LetterInFile>> getGroupedByLetter(NumberOnReceipt[] numberOnReceipts) {
        ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
        Map<Path, List<NumberOnReceipt>> groupedByFile = Arrays.stream(numberOnReceipts).collect(Collectors.groupingBy(NumberOnReceipt::filePath));
        Iterator<List<NumberOnReceipt>> iterator = groupedByFile.values().iterator();

        Map<FontLetter, List<LetterInFile>> groupedByLetter = new HashMap<>();

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
    public static NumberOnReceipt[] loadInputFile() {
        String inputJsonContents = Files.readString(PathsConfig.ALL_NUMBER_WORDS_JSON);
        return JsonUtils
                .OBJECT_MAPPER
                .readValue(inputJsonContents, NumberOnReceipt[].class);
    }

    private static void extractNumberFromFile(NumberOnReceipt numberOnReceipt,
                                              Path imageFile,
                                              String numberText,
                                              Map<FontLetter, List<LetterInFile>> groupedByLetter) {
        List<LetterInFile> letterInFileList = splitToLetters(imageFile, numberOnReceipt);
        int symbolCount = numberText.length();
        if (symbolCount != letterInFileList.size()) {
            log.info("Count of letters in image and in text does not match: {} : {}", imageFile, numberText);
            return;
        }

        for (int i = 0; i < numberText.length(); i++) {
            LetterInFile letterInFile = letterInFileList.get(i);
            FontLetter currentLetter = new FontLetter(numberOnReceipt.type(), numberText.substring(i, i + 1));
            if (SAVE_CATEGORIZED_IMAGES) {
                saveCategorizedLetterImage(currentLetter, imageFile, letterInFile);
            }
            groupedByLetter
                    .computeIfAbsent(currentLetter, k -> new ArrayList<>())
                    .add(letterInFile);
        }
    }

    private static void saveCategorizedLettersWithLargestSameSize(Map<FontLetter, List<LetterInFile>> imageLettersWithLargestMatchingSize) {
        Path parentDir = ROOT_DIR
                .resolve("numbers")
                .resolve("categorised-selected");

        for (Map.Entry<FontLetter, List<LetterInFile>> entry : imageLettersWithLargestMatchingSize.entrySet()) {
            FontLetter currentLetter = entry.getKey();
            List<LetterInFile> images = entry.getValue();
            for (LetterInFile image : images) {
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

    private static void saveCategorizedLetterImage(FontLetter currentLetter, Path imageFile, LetterInFile letterInFile) {
        Path parentDir = ROOT_DIR
                .resolve("numbers")
                .resolve("categorised")
                .resolve(currentLetter.getFont())
                .resolve(currentLetter.getLetter());
        Path filePath = parentDir
                .resolve(String.format("%s_x%d_y%d_xx%d_yy%d", imageFile.getFileName(), letterInFile.x(), letterInFile.y(), letterInFile.endX(), letterInFile.endY()) + ".png");

        createAllDirectories(parentDir);
        IMAGE_CACHE.saveImgLetter(letterInFile, filePath);
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
    private static List<LetterInFile> splitToLetters(Path imageFile, NumberOnReceipt num) {
        MyRect textLocationInFile = num.location();
        List<LetterInFile> result = new ArrayList<>();
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
                    result.add(new LetterInFile(imgStartX, imgStartY, x - 1, imgEndY, num.filePath()));
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

    public static boolean isBlackPixel(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0x00FFFFFF) == 0;
    }
}
