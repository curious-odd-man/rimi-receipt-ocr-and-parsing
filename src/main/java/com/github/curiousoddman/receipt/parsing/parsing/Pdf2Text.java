package com.github.curiousoddman.receipt.parsing.parsing;

import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;
import com.github.curiousoddman.receipt.parsing.tess.MyTesseract;
import com.github.curiousoddman.receipt.parsing.tess.OcrConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Pdf2Text {
    private final MyTesseract tesseract;

    @SneakyThrows
    public MyTessResult convert(OcrConfig ocrConfig, OriginFile originFile) {
        return tesseract.doMyOCR(ocrConfig, originFile);
    }
}
