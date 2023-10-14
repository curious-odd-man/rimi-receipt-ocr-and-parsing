package com.github.curiousoddman.receipt.parsing;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptParser {
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");

    public List<ReceiptRow> parseBills(Stream<Path> files) {
        return files
                .map(Path::toFile)
                .filter(ReceiptParser::isPdfFile)
                .map(ReceiptParser::processFile)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }


    @SneakyThrows
    public static List<ReceiptRow> processFile(File file) {
        try {
            MDC.put("file", file.getName());
            List<ReceiptRow> rows = new ArrayList<>();
            try (PDDocument document = PDDocument.load(file)) {
                String[] lines = getDocumentLines(document);
                ReceiptSections receiptSections = ReceiptSections.BeforeTable;
                for (String line : lines) {
                    log.info("Parsing {} line '{}'", receiptSections, line);
                }
            }
            log.info("Parsed file: ====================== ");
            for (ReceiptRow billRowRaw : rows) {
                log.info("{}", billRowRaw);
            }
            log.info(" ======================");
            return rows;
        } finally {
            MDC.remove("file");
        }
    }

    private static Optional<String> extractByPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(matcher.toMatchResult()
                                      .group(1));
        } else {
            return Optional.empty();
        }
    }

    private static String[] getDocumentLines(PDDocument document) throws IOException {
        if (document.isEncrypted()) {
            throw new IllegalArgumentException("Document is encrypted");
        }
        PDFTextStripper tStripper = new PDFTextStripper();
        tStripper.setSortByPosition(true);
        String text = tStripper.getText(document);
        log.info(text);
        // split by EOL
        return NEWLINE_PATTERN.split(text);
    }

    @SneakyThrows
    public static String decodeText(String input) {
        CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();
        charsetDecoder.onMalformedInput(CodingErrorAction.REPORT);
        return new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), charsetDecoder)).readLine();
    }

    private static boolean isPdfFile(File file) {
        return file.getName().endsWith(".pdf");
    }
}
