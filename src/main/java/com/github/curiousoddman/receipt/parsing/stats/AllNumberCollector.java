package com.github.curiousoddman.receipt.parsing.stats;

import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import com.github.curiousoddman.receipt.parsing.utils.JsonUtils;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class AllNumberCollector {
    public static final Path OUTPUT_PATH = Path.of("D:\\Programming\\git\\caches\\all-number-words.json");

    public record Number(Path filePath, String type, String numberText, MyRect location) {

    }

    public record MyRect(int x, int y, int w, int h) {
        public static MyRect of(Rectangle r) {
            return new MyRect(r.x, r.y, r.width, r.height);
        }
    }

    private final List<Number> numbers = new ArrayList<>();

    public void add(Path filePath, String type, TsvWord word) {
        throwIfWrongType(type);
        numbers.add(new Number(filePath, type, word.getText(), MyRect.of(word.getWordRect())));
    }

    private static void throwIfWrongType(String type) {
        if (type.equals("-0,30")) {
            throw new RuntimeException(type);
        }
    }

    public void add(Path filePath, String type, String text, Rectangle rectangle) {
        throwIfWrongType(type);
        numbers.add(new Number(filePath, type, text, MyRect.of(rectangle)));
    }

    @SneakyThrows
    public void saveResult() {
        String jsonText = JsonUtils.OBJECT_WRITER.writeValueAsString(numbers);
        Files.writeString(OUTPUT_PATH, jsonText, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }
}
