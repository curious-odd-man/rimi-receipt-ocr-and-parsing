import com.github.curiousoddman.receipt.alt.main.Size;
import com.github.curiousoddman.receipt.alt.main.img.FontLetter;
import com.github.curiousoddman.receipt.alt.main.img.ImgMatrix;
import com.github.curiousoddman.receipt.alt.main.img.ImgMatrixCache;
import com.github.curiousoddman.receipt.alt.main.img.LetterInFile;
import com.github.curiousoddman.receipt.parsing.characters.CharImage;
import com.github.curiousoddman.receipt.parsing.characters.CharImages;
import com.github.curiousoddman.receipt.parsing.stats.NumberOnReceipt;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.curiousoddman.receipt.alt.main.NumberImageStatisticsProcessor.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Slf4j
public class TestCharImageRecognition {
    private static final Path                                TEST_DEBUG_INFO_LOCATION = Path.of("W:\\Programming\\git\\caches\\test-results\\TestCharImageRecognition");
    private static final Map<FontLetter, List<LetterInFile>> ALL_S_FILES              = getAllTestFiles("S");
    private static final Map<FontLetter, List<LetterInFile>> ALL_L_FILES              = getAllTestFiles("L");
    private static final Map<FontLetter, List<LetterInFile>> ALL_XL_FILES             = getAllTestFiles("XL");

    private static final ImgMatrixCache imgMatrixCache = new ImgMatrixCache();

    @SneakyThrows
    private static Map<FontLetter, List<LetterInFile>> getAllTestFiles(String dir) {
        NumberOnReceipt[] numberOnReceipts = loadInputFile();
        Map<FontLetter, List<LetterInFile>> groupedByLetter = readOrExtractLetterImages(numberOnReceipts);
        Map<FontLetter, Map<Size, List<LetterInFile>>> imageLettersGroupBySizes = groupEachLetterBySizes(groupedByLetter);

        return imageLettersGroupBySizes
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().getFont().equals(dir))
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), sliceOfEachValueFlattened(entry)))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    private static List<LetterInFile> sliceOfEachValueFlattened(Map.Entry<FontLetter, Map<Size, List<LetterInFile>>> entry) {
        return entry
                .getValue()
                .values()
                .stream()
                .map(l -> l.stream().limit(10).toList())
                .flatMap(List::stream)
                .toList();
    }

    public static Stream<Arguments> getData() {
        return Stream.of(
                             Arrays.stream(CharImages.EXTRA_LARGE_FONT),
                             Arrays.stream(CharImages.LARGE_FONT),
                             Arrays.stream(CharImages.SMALL_FONT)
                     )
                     .flatMap(Function.identity())
                     .flatMap(TestCharImageRecognition::flattenFiles);
    }

    @SneakyThrows
    private static Stream<Arguments> flattenFiles(CharImage charImage) {
        String font = charImage.getFont();
        String character = charImage.getCharacter();
        if (character.equals("comma")) {
            character = ",";
        } else if (character.equals("dash")) {
            character = "-";
        }

        Map<FontLetter, List<LetterInFile>> map = switch (font) {
            case "S" -> ALL_S_FILES;
            case "L" -> ALL_L_FILES;
            case "XL" -> ALL_XL_FILES;
            default -> null;
        };

        String finalCharacter = character;
        return map
                .entrySet()
                .stream()
                .flatMap(entry -> entry
                        .getValue()
                        .stream()
                        .sorted(Comparator.comparing(LetterInFile::filePath))
                        .map(letterInFile -> arguments(charImage, letterInFile, entry.getKey().getLetter().equals(finalCharacter))));
    }

    private static ImgMatrix cache         = null;
    private static Path      imgMatrixPath = null;
    private static int       testIndex     = 0;

    @ParameterizedTest
    @MethodSource("getData")
    void test(CharImage charImage, LetterInFile testFile, boolean expectedToMatch) {
        testIndex++;
        ImgMatrix textImage;
        if (imgMatrixPath == null || !imgMatrixPath.equals(testFile.filePath())) {
            textImage = imgMatrixCache.loadImage(testFile.filePath()).subimage(testFile.x(), testFile.y(), testFile.w(), testFile.h());
            cache = textImage;
            imgMatrixPath = testFile.filePath();
        }

        assertEquals(expectedToMatch, isSimilar(cache, charImage));
    }

    @Test
    void demoTest() {
        Path largeZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_0.png");
        Path smallZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_small_0.png");
        Path nineZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_9.png");
        ImgMatrix image = imgMatrixCache.loadImage(largeZeroPath);
        CharImage largeZero = new CharImage("TEST", "TEST", image);
        assertTrue(isSimilar(image, largeZero));
    }

    @Test
    void demoTest1() {
        Path largeZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_0.png");
        Path smallZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_small_0.png");
        Path nineZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_9.png");
        ImgMatrix zeroImage = imgMatrixCache.loadImage(largeZeroPath);
        ImgMatrix nineImage = imgMatrixCache.loadImage(nineZeroPath);
        CharImage largeZero = new CharImage("TEST", "TEST", nineImage);
        assertFalse(isSimilar(zeroImage, largeZero));
    }

    @Test
    void demoTest2() {
        Path largeZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_0.png");
        Path smallZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_small_0.png");
        Path nineZeroPath = Path.of("W:\\Programming\\git\\private-tools\\receipts-parsing\\src\\test\\resources\\Demo_9.png");
        ImgMatrix zeroImage = imgMatrixCache.loadImage(largeZeroPath);
        ImgMatrix smallZeroImage = imgMatrixCache.loadImage(smallZeroPath);
        CharImage largeZero = new CharImage("TEST", "TEST", smallZeroImage);
        assertFalse(isSimilar(zeroImage, largeZero));
    }

    private static boolean isSimilar(ImgMatrix textImage, CharImage charImage) {
        ImgMatrix templateImage = charImage.getImage();
        int totalPixels = (templateImage.getHeight() + 20) * (templateImage.getWidth() + 20);
        int bestDifference = totalPixels;
        for (int imgX = 0; imgX < textImage.getWidth(); imgX++) {
            for (int imgY = 0; imgY < textImage.getHeight(); imgY++) {
                List<Point> diff = textImage.compareToOtherAt(templateImage, imgX, imgY);
                if (diff.size() < bestDifference) {
                    log.info("Found new best match at {}:{}", imgX, imgY);
                    bestDifference = diff.size();
                    saveToFile(totalPixels, bestDifference, diff, textImage, charImage, imgX, imgY);
                }
            }
        }

        double errorsRate = bestDifference / ((double) totalPixels);
        boolean isMatching = errorsRate < 0.05;
        log.info("Difference score: {}/{}: {}", bestDifference, totalPixels, errorsRate);
        return isMatching;
    }

    @SneakyThrows
    private static void saveToFile(int totalPixels,
                                   int bestDifference,
                                   List<Point> diff,
                                   ImgMatrix textImage,
                                   CharImage charImage,
                                   int imgX,
                                   int imgY) {
        var resultDirectory = TEST_DEBUG_INFO_LOCATION
                .resolve(charImage.getFont())
                .resolve(charImage.getCharacter())
                .resolve(testIndex + "");

        double errorRate = bestDifference / ((double) totalPixels);

        StringBuilder sb = new StringBuilder();
        sb.append("Total Pixels: ").append(totalPixels).append('\n');
        sb.append("Difference: ").append(bestDifference).append('\n');
        sb.append("Error Rate: ").append(errorRate).append('\n');
        sb.append("Comparing at X:").append(imgX).append("; Y:").append(imgY).append('\n');
        sb.append("Image of text").append('\n');
        sb.append(textImage.toString()).append('\n');
        sb.append("Image of template").append('\n');
        sb.append(charImage.getImage().toString()).append('\n');
        sb.append("Differences ").append('\n');
        for (Point point : diff) {
            sb.append(point).append('\n');
        }

        Files.createDirectories(resultDirectory);
        Files.writeString(resultDirectory.resolve(String.format("X%d_Y%d.txt", imgX, imgY)), sb.toString());
    }
}
