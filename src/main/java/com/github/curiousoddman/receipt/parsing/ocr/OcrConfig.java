package com.github.curiousoddman.receipt.parsing.ocr;

import lombok.Builder;
import lombok.Getter;
import net.sourceforge.tess4j.ITessAPI;

import java.awt.*;
import java.nio.file.Path;

import static net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_AUTO_OSD;


@Getter
@Builder(builderMethodName = "hiddenBuilder")
public class OcrConfig {
    private final Path      tiffFile;
    private final Rectangle ocrArea;
    @Builder.Default
    private final boolean   ocrToTsv      = false;
    @Builder.Default
    private final boolean   ocrDigitsOnly = false;
    @Builder.Default
    private final int       pageSegMode   = PSM_AUTO_OSD;
    @Builder.Default
    private final int       ocrEngineMode = OEM_LSTM_ONLY;

    public static OcrConfigBuilder builder(Path tiffFile) {
        return hiddenBuilder().tiffFile(tiffFile);
    }

    public void apply(OcrService ocrService) {
        ocrService.setVariable("tessedit_char_blacklist", "_—");
        if (ocrDigitsOnly) {
            ocrService.setPageSegMode(ITessAPI.TessPageSegMode.PSM_RAW_LINE);
            ocrService.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY);
            ocrService.setVariable("tessedit_char_whitelist", "-.,0123456789");
        } else {

            if (ocrArea != null) {
                ocrService.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK);
                ocrService.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_DEFAULT);
            } else {
                ocrService.setPageSegMode(pageSegMode);
                ocrService.setOcrEngineMode(ocrEngineMode);
            }

            ocrService.getProperties().remove("tessedit_char_whitelist");
        }
    }
}
