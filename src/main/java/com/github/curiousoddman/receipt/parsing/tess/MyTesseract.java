package com.github.curiousoddman.receipt.parsing.tess;

import com.github.curiousoddman.receipt.parsing.cache.FileCache;
import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.sun.jna.Pointer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageIOHelper;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Properties;

@Slf4j
@Component
public class MyTesseract extends Tesseract {
    public MyTesseract(FileCache fileCache) {
        setDatapath("D:\\Programming\\git\\private-tools\\receipts-parsing\\tes");
        setLanguage("lav");
    }

    public MyTessResult doMyOCR(OcrConfig ocrConfig, OriginFile originFile) throws TesseractException {
        try {
            ocrConfig.apply(this);
            File tiffFile = ocrConfig.getTiffFile().toFile();
            String tiffFilePath = ocrConfig.getTiffFile().toAbsolutePath().toString();
            String imageFileFormat = ImageIOHelper.getImageFileFormat(tiffFile);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(imageFileFormat);
            ImageReader reader = readers.next();
            StringBuilder plainTextResult = new StringBuilder();
            StringBuilder tsvTextResult = new StringBuilder();
            try (ImageInputStream iis = ImageIO.createImageInputStream(tiffFile)) {
                reader.setInput(iis);
                int imageTotal = reader.getNumImages(true);

                init();
                setVariables();

                for (int i = 0; i < imageTotal; i++) {
                    IIOImage oimage = reader.readAll(i, reader.getDefaultReadParam());
                    plainTextResult.append(doOCR(oimage, tiffFilePath, i + 1, false, null));
                    tsvTextResult.append(doOCR(oimage, tiffFilePath, i + 1, true, null));
                }


            } finally {
                reader.dispose();
                dispose();
            }

            return new MyTessResult(
                    originFile,
                    plainTextResult.toString(),
                    tsvTextResult.toString()
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    @SneakyThrows
    public static File getImageFile(File inputFile) {
        return ImageIOHelper.getImageFile(inputFile);
    }

    @Override
    public String doOCR(File inputFile, Rectangle rect) {
        throw new UnsupportedOperationException();
    }

    @SneakyThrows
    public Properties getProperties() {
        try {
            Field props = this.getClass().getSuperclass().getDeclaredField("prop");
            props.setAccessible(true);
            return (Properties) props.get(this);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new Properties();
        }
    }

    public String doOCR(OcrConfig ocrConfig) throws TesseractException {
        ocrConfig.apply(this);
        File tiffFile = ocrConfig.getTiffFile().toFile();
        String tiffFilePath = ocrConfig.getTiffFile().toAbsolutePath().toString();
        try {
            String imageFileFormat = ImageIOHelper.getImageFileFormat(tiffFile);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(imageFileFormat);
            ImageReader reader = readers.next();
            StringBuilder result = new StringBuilder();
            try (ImageInputStream iis = ImageIO.createImageInputStream(tiffFile);) {
                reader.setInput(iis);
                int imageTotal = reader.getNumImages(true);

                init();
                setVariables();

                for (int i = 0; i < imageTotal; i++) {
                    IIOImage oimage = reader.readAll(i, reader.getDefaultReadParam());
                    result.append(doOCR(oimage, tiffFilePath, i + 1, ocrConfig.isOcrToTsv(), ocrConfig.getOcrArea()));
                }
            } finally {
                reader.dispose();
                dispose();
            }

            return result.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    private String doOCR(IIOImage oimage,
                         String filename,
                         int pageNum,
                         boolean isTsv,
                         Rectangle rect) {
        String text = "";

        try {
            setImage(oimage.getRenderedImage(), rect);
            ITessAPI.TessBaseAPI handle = getHandle();
            TessAPI api = getAPI();
            if (filename != null && !filename.isEmpty()) {
                getAPI().TessBaseAPISetInputName(getHandle(), filename);
            }
            Pointer textPtr;
            if (isTsv) {
                textPtr = api.TessBaseAPIGetTsvText(handle, pageNum - 1);
            } else {
                textPtr = api.TessBaseAPIGetUTF8Text(handle);
            }

            String str = textPtr.getString(0);
            api.TessDeleteText(textPtr);
            return str;
        } catch (IOException ioe) {
            // skip the problematic image
            log.warn(ioe.getMessage(), ioe);
        }

        return text;
    }
}
