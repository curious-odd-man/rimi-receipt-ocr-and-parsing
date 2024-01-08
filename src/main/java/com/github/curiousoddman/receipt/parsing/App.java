package com.github.curiousoddman.receipt.parsing;

import com.github.curiousoddman.receipt.parsing.cache.FileCache;
import com.github.curiousoddman.receipt.parsing.config.PathsConfig;
import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi.RimiText2Receipt;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.Tsv2Struct;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvDocument;
import com.github.curiousoddman.receipt.parsing.stats.ReceiptStatsCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.tess.TesseractConfig;
import com.github.curiousoddman.receipt.parsing.validation.ValidationExecutor;
import com.github.curiousoddman.receipt.parsing.validation.ValidationStatsCollector;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.curiousoddman.receipt.parsing.utils.JsonUtils.OBJECT_WRITER;


@Slf4j
@Component
@RequiredArgsConstructor
public class App implements ApplicationRunner {
    private static final int                      MAX_PARALLEL_THREADS   = 5;
    private static final ThreadLocal<MyTesseract> TESSERACT_THREAD_LOCAL = ThreadLocal
            .withInitial(() -> {
                log.info("New Tesseract created");
                return new MyTesseract();
            });

    private final RimiText2Receipt            rimiText2Receipt;
    private final FileCache                   fileCache;
    private final IgnoreList                  ignoreList;
    private final Whitelist                   whitelist;
    private final ValidationExecutor          validationExecutor;
    private final Tsv2Struct                  tsv2Struct;
    private final List<ReceiptStatsCollector> receiptStatsCollectors;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ValidationStatsCollector validationStatsCollector = new ValidationStatsCollector();
        try (Stream<Path> files = Files.list(PathsConfig.PDF_INPUT_DIR)) {
            List<Path> allPdfFiles = files.filter(App::isPdfFile).toList();
            if (MAX_PARALLEL_THREADS == 0) {
                for (Path pdfFile : allPdfFiles) {
                    safeTransformFile(pdfFile, validationStatsCollector);
                }
            } else {
                ExecutorService executorService = Executors.newFixedThreadPool(MAX_PARALLEL_THREADS);
                for (Path pdfFile : allPdfFiles) {
                    executorService.submit(() -> safeTransformFile(pdfFile, validationStatsCollector));
                }
                executorService.shutdown();
                while (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.info("Still running...");
                }
            }
        }

        receiptStatsCollectors.forEach(ReceiptStatsCollector::printSummary);
        validationExecutor.saveResult(PathsConfig.VALIDATION_RESULT_JSON);
    }

    private void safeTransformFile(Path pdfFile, ValidationStatsCollector validationStatsCollector) {
        try {
            transformFile(pdfFile, validationStatsCollector);
        } catch (Exception e) {
            log.error("Unexepcted error", e);
        }
    }

    @SneakyThrows
    private void transformFile(Path pdfFile, ValidationStatsCollector validationStatsCollector) {
        String sourcePdfName = pdfFile.toFile().getName();
        MDC.put("file", sourcePdfName);

        if (ignoreList.isIgnored(sourcePdfName)) {
            log.info("Skipping file {} due to ignore list", sourcePdfName);
            return;
        }

        if (!whitelist.isWhitelisted(sourcePdfName)) {
            return;
        }

        log.info("Starting...");

        MyTessResult myTessResult = fileCache.getOrCreate(pdfFile, this::runOcr);
        TsvDocument tsvDocument = tsv2Struct.parseTsv(myTessResult.getTsvText());
        myTessResult.setTsvDocument(tsvDocument);
        fileCache.create(Path.of(sourcePdfName + ".tsv.json"), OBJECT_WRITER.writeValueAsString(tsvDocument));
        Receipt receipt = rimiText2Receipt.parse(sourcePdfName, myTessResult, TESSERACT_THREAD_LOCAL.get());
        String receiptJson = OBJECT_WRITER.writeValueAsString(receipt);

        fileCache.create(Path.of(sourcePdfName + ".json"), receiptJson);

        validationExecutor.execute(validationStatsCollector, receipt);
        receiptStatsCollectors.forEach(collector -> collector.collect(receipt));

        log.info("Completed");
    }

    @SneakyThrows
    private MyTessResult runOcr(TesseractConfig tesseractConfig, OriginFile originFile) {
        return TESSERACT_THREAD_LOCAL.get().doMyOCR(tesseractConfig, originFile);

    }

    private static boolean isPdfFile(Path file) {
        return file.toFile().getName().endsWith(".pdf");
    }
}
