package com.github.curiousoddman.receipt.parsing;

import com.github.curiousoddman.receipt.parsing.parsing.Pdf2Text;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.Text2Receipt;
import com.github.curiousoddman.receipt.parsing.validation.ReceiptValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final Pdf2Text               pdf2Text;
    private final List<Text2Receipt>     text2ReceiptList;
    private final List<ReceiptValidator> receiptValidatorList;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Stream<Path> files = Files.list(Paths.get("D:\\Programming\\git\\private-tools\\gmail-client\\output"))) {
            files
                    .filter(App::isPdfFile)
                    .map(pdf2Text::getOrConvert)
                    .map(text -> text2ReceiptList.stream().map(parser -> parser.parse(text)).filter(Objects::nonNull).findFirst())
                    .map(Optional::get)
                    .forEach(receipt -> receiptValidatorList.forEach(receiptValidator -> receiptValidator.validate(receipt)));
        }
    }

    private static boolean isPdfFile(Path file) {
        return file.toFile().getName().endsWith(".pdf");
    }
}
