package com.github.curiousoddman.receipt.parsing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.curiousoddman.receipt.parsing.cache.FileCache;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.Pdf2Text;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.Text2Receipt;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.validation.ReceiptValidator;
import com.github.curiousoddman.receipt.parsing.validation.ValidationStatus;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;


@Slf4j
@Component
@RequiredArgsConstructor
public class App implements ApplicationRunner {
    private static final ObjectMapper           OBJECT_MAPPER = JsonMapper.builder()
                                                                          .addModule(new ParameterNamesModule())
                                                                          .addModule(new Jdk8Module())
                                                                          .addModule(new JavaTimeModule())
                                                                          .build();
    private final        Pdf2Text               pdf2Text;
    private final        List<Text2Receipt>     text2ReceiptList;
    private final        List<ReceiptValidator> receiptValidators;
    private final        FileCache              fileCache;
    private final        IgnoreList             ignoreList;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Stream<Path> files = Files.list(Paths.get("D:\\Programming\\git\\private-tools\\gmail-client\\output"))) {
            List<Path> allPdfFiles = files.filter(App::isPdfFile).toList();
            for (Path file : allPdfFiles) {
                String sourcePdfName = file.toFile().getName();
                MDC.put("file", sourcePdfName);
                if (ignoreList.isIgnored(sourcePdfName)) {
                    continue;
                }
                MyTessResult imageAsText = fileCache.getOrCreate("raw-text", sourcePdfName, () -> pdf2Text.convert(file));
                Optional<Receipt> optionalReceipt = parseWithAnyParser(sourcePdfName, imageAsText);
                if (optionalReceipt.isPresent()) {
                    Receipt receipt = optionalReceipt.get();
                    String receiptJson = OBJECT_MAPPER
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(receipt);

                    fileCache.create("parsed-receipt",
                                     sourcePdfName + ".txt",
                                     receiptJson);
                    ValidationStatus validationResult = ValidationStatus.SUCCESS;
                    for (ReceiptValidator receiptValidator : receiptValidators) {
                        validationResult = validationResult.merge(receiptValidator.validate(receipt));
                    }
                    if (validationResult == ValidationStatus.SUCCESS) {
                        log.info("Successfully validated receipt {}", sourcePdfName);
                    }
                } else {
                    log.error("Failed to parse receipt {}", sourcePdfName);
                }
            }
        }
    }


    @SneakyThrows
    private Optional<Receipt> parseWithAnyParser(String fileName, MyTessResult text) {
        return text2ReceiptList
                .stream()
                .map(parser -> tryParseOrNull(fileName, text, parser))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static Receipt tryParseOrNull(String fileName, MyTessResult text, Text2Receipt parser) {
        try {
            return parser.parse(fileName, text);
        } catch (Exception e) {
            log.error("Failed to parse receipt with parser {}", parser.getClass(), e);
            return null;
        }
    }

    private static boolean isPdfFile(Path file) {
        return file.toFile().getName().endsWith(".pdf");
    }
}
