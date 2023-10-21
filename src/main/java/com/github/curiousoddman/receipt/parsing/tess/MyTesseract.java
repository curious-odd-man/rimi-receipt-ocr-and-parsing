package com.github.curiousoddman.receipt.parsing.tess;

import com.github.curiousoddman.receipt.parsing.cache.FileCache;
import com.sun.jna.Pointer;
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
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Slf4j
@Component
public class MyTesseract extends Tesseract {

    private final FileCache fileCache;

    public MyTesseract(FileCache fileCache) {
        setDatapath("D:\\Programming\\git\\private-tools\\receipts-parsing\\tes");
        setLanguage("lav");
        setPageSegMode(1);
        setOcrEngineMode(1);
        setVariable("tessedit_create_tsv", "1");
        this.fileCache = fileCache;
    }

    public MyTessResult doMyOCR(File inputFile) throws TesseractException {
        try {
            File imageFile = fileCache.getOrCreateFile(inputFile.getName() + ".tiff", () -> {
                try {
                    return ImageIOHelper.getImageFile(inputFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            String imageFileFormat = ImageIOHelper.getImageFileFormat(imageFile);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(imageFileFormat);
            if (!readers.hasNext()) {
                throw new RuntimeException(ImageIOHelper.JAI_IMAGE_READER_MESSAGE);
            }
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
                    plainTextResult.append(doOCR(oimage, inputFile.getPath(), i + 1, false));
                    tsvTextResult.append(doOCR(oimage, inputFile.getPath(), i + 1, true));
                }


            } finally {
                // delete temporary TIFF image for PDF
                if (imageFile != null
                        && imageFile.exists()
                        && imageFile != inputFile
                        && imageFile.getName().startsWith("multipage")
                        && imageFile.getName().endsWith(ImageIOHelper.TIFF_EXT)) {
                    imageFile.delete();
                }
                reader.dispose();
                dispose();
            }

            return new MyTessResult(
                    imageFile,
                    plainTextResult.toString(),
                    tsvTextResult.toString()
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    private String doOCR(IIOImage oimage,
                         String filename,
                         int pageNum,
                         boolean isTsv) {
        String text = "";

        try {
            setImage(oimage.getRenderedImage(), null);
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
