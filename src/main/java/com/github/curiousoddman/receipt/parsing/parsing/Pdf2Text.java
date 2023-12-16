package com.github.curiousoddman.receipt.parsing.parsing;

import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class Pdf2Text {
    private final MyTesseract tesseract;

    @SneakyThrows
    public MyTessResult convert(Path path) {
        return tesseract.doMyOCR(path);
    }
}
