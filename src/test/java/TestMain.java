import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.nio.file.Path;

public class TestMain {
    public static void main(String[] args) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("D:\\Programming\\git\\private-tools\\receipts-parsing\\tes");
        tesseract.setLanguage("lav");
//        tesseract.setPageSegMode(1);
//        tesseract.setOcrEngineMode(1);
        String txt = tesseract.doOCR(Path.of("D:\\Programming\\git\\caches\\cache\\2-226761.pdf.tiff").toFile());
//        String txt = tesseract.doOCR(Path.of("D:\\Programming\\git\\caches\\cache\\2-226761.pdf.tiff").toFile(),
//                                     new Rectangle(1156, 943, 123, 44));
        System.out.println(txt);
    }
}
