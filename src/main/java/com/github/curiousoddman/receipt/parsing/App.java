package com.github.curiousoddman.receipt.parsing;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.ocr.OcrResult;
import com.github.curiousoddman.receipt.parsing.ocr.OcrServiceProvider;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi.RimiText2Receipt;
import com.github.curiousoddman.receipt.parsing.utils.PathsUtils;
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
    private static final int MAX_PARALLEL_THREADS = 5;

    private final RimiText2Receipt   rimiText2Receipt;
    private final IgnoreList         ignoreList;
    private final Whitelist          whitelist;
    private final ValidationExecutor validationExecutor;
    private final OcrServiceProvider ocrServiceProvider;
    private final PathsUtils         pathsUtils;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ValidationStatsCollector validationStatsCollector = new ValidationStatsCollector();
        try (Stream<Path> files = Files.list(pathsUtils.getPdfInputDir())) {
            List<Path> allPdfFiles = files.filter(PathsUtils::isPdfFile).toList();
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

        validationExecutor.saveResult(pathsUtils.getValidationResultPath());
    }

    private void safeTransformFile(Path pdfFile, ValidationStatsCollector validationStatsCollector) {
        try {
            transformFile(pdfFile, validationStatsCollector);
        } catch (Exception e) {
            log.error("Unexpected error", e);
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

        OcrResult ocrResult = ocrServiceProvider.get().getCachedOrDoOcr(pdfFile);
        Receipt receipt = rimiText2Receipt.parse(sourcePdfName, ocrResult, ocrServiceProvider.get());

        String receiptJson = OBJECT_WRITER.writeValueAsString(receipt);
        Files.writeString(ocrResult.cacheDir().resolve(sourcePdfName + ".json"), receiptJson);

        validationExecutor.execute(validationStatsCollector, receipt);
        log.info("Completed");
    }
}
