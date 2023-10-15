package com.github.curiousoddman.receipt.parsing;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptParser {

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

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("D:\\Programming\\git\\private-tools\\receipts-parsing\\tes");
            tesseract.setLanguage("lav");
            tesseract.setPageSegMode(1);
            tesseract.setOcrEngineMode(1);
            String result = tesseract.doOCR(file);
            log.info("result = ");
            log.info("{}", result);


            List<ReceiptRow> rows = new ArrayList<>();
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

    private static boolean isPdfFile(File file) {
        return file.getName().endsWith(".pdf");
    }
}
