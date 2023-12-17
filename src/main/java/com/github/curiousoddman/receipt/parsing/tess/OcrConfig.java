package com.github.curiousoddman.receipt.parsing.tess;

import lombok.Builder;
import lombok.Getter;
import net.sourceforge.tess4j.ITessAPI;

import java.awt.*;
import java.nio.file.Path;

import static net.sourceforge.tess4j.ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_AUTO_OSD;

@Builder(builderMethodName = "hiddenBuilder")
@Getter
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

    public void apply(MyTesseract myTesseract) {
        myTesseract.setVariable("tessedit_char_blacklist", "_");
        if (ocrDigitsOnly) {
            myTesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_RAW_LINE);
            myTesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_TESSERACT_ONLY);
            myTesseract.setVariable("tessedit_char_whitelist", "-.,0123456789");
        } else {

            if (ocrArea != null) {
                myTesseract.setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK);
                myTesseract.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_DEFAULT);
            } else {
                myTesseract.setPageSegMode(pageSegMode);
                myTesseract.setOcrEngineMode(ocrEngineMode);
            }

            myTesseract.getProperties().remove("tessedit_char_whitelist");
        }
    }
}
