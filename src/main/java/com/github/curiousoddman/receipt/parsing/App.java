package com.github.curiousoddman.receipt.parsing;

import com.github.curiousoddman.receipt.parsing.cache.FileCache;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.Pdf2Text;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.Text2Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import com.github.curiousoddman.receipt.parsing.stats.AllNumberCollector;
import com.github.curiousoddman.receipt.parsing.stats.ParsingStatsCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.validation.ValidationExecutor;
import com.github.curiousoddman.receipt.parsing.validation.ValidationStatsCollector;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.curiousoddman.receipt.parsing.utils.JsonUtils.OBJECT_WRITER;


@Slf4j
@Component
@RequiredArgsConstructor
public class App implements ApplicationRunner {
    private final Pdf2Text           pdf2Text;
    private final List<Text2Receipt> text2ReceiptList;
    private final FileCache          fileCache;
    private final IgnoreList         ignoreList;
    private final Whitelist          whitelist;
    private final ValidationExecutor validationExecutor;
    private final Tsv2Struct         tsv2Struct;
    private final AllNumberCollector allNumberCollector;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ValidationStatsCollector validationStatsCollector = new ValidationStatsCollector();
        ParsingStatsCollector parsingStatsCollector = new ParsingStatsCollector();
        try (Stream<Path> files = Files.list(Paths.get("D:\\Programming\\git\\private-tools\\gmail-client\\output"))) {
            List<Path> allPdfFiles = files.filter(App::isPdfFile).toList();
            for (Path pdfFile : allPdfFiles) {
                String sourcePdfName = pdfFile.toFile().getName();
                MDC.put("file", sourcePdfName);

                if (ignoreList.isIgnored(sourcePdfName)) {
                    log.info("Skipping file {} due to ignore list", sourcePdfName);
                    continue;
                }

                if (!whitelist.isWhitelisted(sourcePdfName)) {
                    continue;
                }

                log.info("Starting...");

                MyTessResult myTessResult = fileCache.getOrCreate(pdfFile, pdf2Text::convert);
                TsvDocument tsvDocument = tsv2Struct.parseTsv(myTessResult.getTsvText());
                myTessResult.setTsvDocument(tsvDocument);
                fileCache.create(Path.of(sourcePdfName + ".tsv.json"), OBJECT_WRITER.writeValueAsString(tsvDocument));
                Optional<Receipt> optionalReceipt = parseWithAnyParser(sourcePdfName, myTessResult, parsingStatsCollector);
                if (optionalReceipt.isPresent()) {
                    Receipt receipt = optionalReceipt.get();
                    String receiptJson = OBJECT_WRITER.writeValueAsString(receipt);

                    fileCache.create(Path.of(sourcePdfName + ".json"), receiptJson);

                    validationExecutor.execute(validationStatsCollector, receipt);
                } else {
                    log.error("Failed to parse receipt {}", sourcePdfName);
                }
            }
        }

        parsingStatsCollector.printStats();
        allNumberCollector.saveResult();
    }


    private Optional<Receipt> parseWithAnyParser(String fileName,
                                                 MyTessResult myTessResult,
                                                 ParsingStatsCollector parsingStatsCollector) {
        return text2ReceiptList
                .stream()
                .map(parser -> tryParseOrNull(fileName, myTessResult, parser, parsingStatsCollector))
                .filter(Objects::nonNull)
                .findFirst();
    }

    private static Receipt tryParseOrNull(String fileName,
                                          MyTessResult myTessResult,
                                          Text2Receipt parser,
                                          ParsingStatsCollector parsingStatsCollector) {
        try {
            return parser.parse(fileName, myTessResult, parsingStatsCollector);
        } catch (Exception e) {
            log.error("Failed to parse receipt with parser {}", parser.getClass(), e);
            return null;
        }
    }

    private static boolean isPdfFile(Path file) {
        return file.toFile().getName().endsWith(".pdf");
    }
}
