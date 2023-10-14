package com.github.curiousoddman.receipt.parsing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;


@Slf4j
@Component
@RequiredArgsConstructor
public class App implements ApplicationRunner {
    private final ReceiptParser receiptParser;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Stream<Path> files = Files.list(Paths.get("D:\\Programming\\git\\private-tools\\receipts-parsing\\data\\input"))) {
            List<ReceiptRow> rows = receiptParser.parseBills(files);
            log.info("All parsed rows");
            for (ReceiptRow row : rows) {
                log.info("\t{}", row);
            }
            //MyCsvWriter.writeCsv(rows, ReceiptRow.FIELD_EXTRACTORS);
        }
    }
}
