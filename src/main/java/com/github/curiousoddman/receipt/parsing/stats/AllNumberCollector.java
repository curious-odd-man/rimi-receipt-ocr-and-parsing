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
    public static final Path ALL_NUMBER_WORDS_JSON = Path.of("D:\\Programming\\git\\caches\\all-number-words.json");

    private final List<NumberOnReceipt> numberOnReceipts = new ArrayList<>();

    public void add(Path filePath, String type, TsvWord word) {
        numberOnReceipts.add(new NumberOnReceipt(filePath, type, word.getText(), MyRect.of(word.getWordRect())));
    }

    public void add(Path filePath, String type, String text, Rectangle rectangle) {
        numberOnReceipts.add(new NumberOnReceipt(filePath, type, text, MyRect.of(rectangle)));
    }

    @SneakyThrows
    public void saveResult() {
        String jsonText = JsonUtils.OBJECT_WRITER.writeValueAsString(numberOnReceipts);
        Files.writeString(ALL_NUMBER_WORDS_JSON, jsonText, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }
}
