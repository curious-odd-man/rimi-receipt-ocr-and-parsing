package com.github.curiousoddman.receipt.parsing.tess;

import com.github.curiousoddman.receipt.parsing.cache.FileCache;
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

    private final FileCache fileCache;

    public MyTesseract(FileCache fileCache) {
        setDatapath("D:\\Programming\\git\\private-tools\\receipts-parsing\\tes");
        setLanguage("lav");
        this.fileCache = fileCache;
    }

    public MyTessResult doMyOCR(File inputFile) throws TesseractException {
        try {
            setPageSegMode(1);
            setOcrEngineMode(1);
            File imageFile = fileCache.getOrCreateFile(inputFile.getName() + ".tiff", () -> getImageFile(inputFile));
            String imageFileFormat = ImageIOHelper.getImageFileFormat(imageFile);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(imageFileFormat);
            ImageReader reader = readers.next();
            StringBuilder plainTextResult = new StringBuilder();
            StringBuilder tsvTextResult = new StringBuilder();
            try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile);) {
                reader.setInput(iis);
                int imageTotal = reader.getNumImages(true);

                init();
                setVariables();

                for (int i = 0; i < imageTotal; i++) {
                    IIOImage oimage = reader.readAll(i, reader.getDefaultReadParam());
                    plainTextResult.append(doOCR(oimage, inputFile.getPath(), i + 1, false, null));
                    tsvTextResult.append(doOCR(oimage, inputFile.getPath(), i + 1, true, null));
                }


            } finally {
                // delete temporary TIFF image for PDF
                deleteTmpFile(inputFile, imageFile);
                reader.dispose();
                dispose();
            }

            return new MyTessResult(
                    inputFile,
                    plainTextResult.toString(),
                    tsvTextResult.toString()
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    @SneakyThrows
    private static File getImageFile(File inputFile) {
        return ImageIOHelper.getImageFile(inputFile);
    }

    @Override
    public String doOCR(File inputFile, Rectangle rect) throws TesseractException {
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

    public String doOCR(File inputFile, Rectangle rect, boolean isTsv, boolean isDigitsOnly) throws TesseractException {
        setPageSegMode(ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK);
        setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_DEFAULT);
        if (isDigitsOnly) {
            //baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, );
            setVariable("tessedit_char_whitelist", "-.,0123456789");
        } else {
            getProperties().remove("tessedit_char_whitelist");
        }
        try {
            String fileName = inputFile.getName();
            if (!fileName.endsWith(".tiff")) {
                fileName += ".tiff";
            }
            File imageFile = fileCache.getOrCreateFile(fileName, () -> getImageFile(inputFile));
//            int x = rect.x;
//            int y = rect.y;
//            int width = rect.width;
//            int height = rect.height;
//            String rectangledFileName = inputFile + String.format("_%d_%d_%d_%d.tiff", x, y, width, height);
//            fileCache.getOrCreateFile(rectangledFileName, () -> getFileWithRectangle(inputFile, rectangledFileName, x, y, width, height));

            String imageFileFormat = ImageIOHelper.getImageFileFormat(imageFile);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(imageFileFormat);
            ImageReader reader = readers.next();
            StringBuilder result = new StringBuilder();
            try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile);) {
                reader.setInput(iis);
                int imageTotal = reader.getNumImages(true);

                init();
                setVariables();

                for (int i = 0; i < imageTotal; i++) {
                    IIOImage oimage = reader.readAll(i, reader.getDefaultReadParam());
                    result.append(doOCR(oimage, inputFile.getPath(), i + 1, isTsv, rect));
                }
            } finally {
                deleteTmpFile(inputFile, imageFile);
                reader.dispose();
                dispose();
            }

            return result.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    private static void deleteTmpFile(File inputFile, File imageFile) {
        if (imageFile != null
                && imageFile.exists()
                && imageFile != inputFile
                && imageFile.getName().startsWith("multipage")
                && imageFile.getName().endsWith(ImageIOHelper.TIFF_EXT)) {
            imageFile.delete();
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
