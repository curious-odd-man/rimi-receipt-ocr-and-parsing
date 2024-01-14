package com.github.curiousoddman.receipt.parsing.ocr;

import com.github.curiousoddman.receipt.parsing.config.DebugConfig;
import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.TsvParser;
import com.github.curiousoddman.receipt.parsing.ocr.tsv.document.OcrTsvResult;
import com.github.curiousoddman.receipt.parsing.utils.ImageUtils;
import com.github.curiousoddman.receipt.parsing.utils.PathsUtils;
import com.sun.jna.Pointer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.TessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.ImageIOHelper;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Properties;

import static com.github.curiousoddman.receipt.parsing.utils.ImageUtils.getImageFile;
import static com.github.curiousoddman.receipt.parsing.utils.ImageUtils.saveFileWithRectangle;
import static com.github.curiousoddman.receipt.parsing.utils.JsonUtils.OBJECT_WRITER;

@Slf4j
public class OcrService extends Tesseract {
    private final PathsUtils  pathsUtils;
    private final TsvParser   tsvParser;
    private final DebugConfig debugConfig;

    @SneakyThrows
    public OcrService(PathsUtils pathsUtils, TsvParser tsvParser, DebugConfig debugConfig) {
        this.pathsUtils = pathsUtils;
        this.tsvParser = tsvParser;
        this.debugConfig = debugConfig;
        Files.createDirectories(ocrCachesRoot(pathsUtils));
        setDatapath(pathsUtils.getTesseractModelPath());
        setLanguage("lav");
    }

    @SneakyThrows
    public OcrResult getCachedOrDoOcr(Path pdfFile) {
        Path pdfFileName = pdfFile.getFileName();
        Path newRoot = getSubdirectoryPath(pdfFile);
        Path textCacheFilePath = newRoot.resolve(pdfFileName + ".txt");
        Path tsvCacheFilePath = newRoot.resolve(pdfFileName + ".tsv");
        Path imageCacheFilePath = newRoot.resolve(pdfFileName + ".tiff");
        Path preprocessedImagePath = newRoot.resolve(pdfFileName + ".preprocessed.tiff");
        Path tsvDocumentFilePath = newRoot.resolve(pdfFileName + ".tsv.json");
        OriginFile originFile = new OriginFile(pdfFile, imageCacheFilePath, preprocessedImagePath);
        if (Files.exists(textCacheFilePath) && Files.exists(tsvCacheFilePath)) {
            return new OcrResult(
                    originFile,
                    Files.readString(textCacheFilePath),
                    tsvParser.parse(Files.readString(tsvCacheFilePath))
            );
        }

        if (!Files.exists(imageCacheFilePath)) {
            File imageFile = getImageFile(pdfFile.toFile());
            Files.copy(imageFile.toPath(), imageCacheFilePath);
        }

        if (!Files.exists(preprocessedImagePath)) {
            ImageUtils.doImagePreprocessing(imageCacheFilePath, preprocessedImagePath);
        }

        OcrConfig ocrConfig = OcrConfig.builder(originFile.preprocessedTiff()).build();
        OcrResult tessResult = doMyOCR(ocrConfig, originFile);
        Files.writeString(textCacheFilePath, tessResult.plainText());
        Files.writeString(tsvCacheFilePath, tessResult.ocrTsvResult().getTsvFileContents());
        Files.writeString(tsvDocumentFilePath, OBJECT_WRITER.writeValueAsString(tessResult.ocrTsvResult()));
        return tessResult;
    }

    public OcrResult doMyOCR(OcrConfig ocrConfig, OriginFile originFile) throws TesseractException {
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

            OcrTsvResult ocrTsvResult = tsvParser.parse(tsvTextResult.toString());

            return new OcrResult(
                    originFile,
                    plainTextResult.toString(),
                    ocrTsvResult
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new TesseractException(e);
        }
    }

    @Override
    public String doOCR(File inputFile, Rectangle rect) {
        throw new UnsupportedOperationException();
    }

    @SneakyThrows
    public Properties getProperties() {
        try {
            Field props = getClass().getSuperclass().getDeclaredField("prop");
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

        if (debugConfig.isSaveReOcrAreaImages()) {
            Rectangle rect = ocrConfig.getOcrArea();
            if (rect != null) {
                int x = rect.x;
                int y = rect.y;
                int width = rect.width;
                int height = rect.height;
                Path rectangledFileName = Path.of(ocrConfig.getTiffFile() + String.format("_%d_%d_%d_%d.tiff", x, y, width, height));
                log.info("Saving rectangled file: {}", rectangledFileName.toAbsolutePath());
                saveFileWithRectangle(tiffFile, rectangledFileName, x, y, width, height);
            }
        }

        try {
            String imageFileFormat = ImageIOHelper.getImageFileFormat(tiffFile);
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(imageFileFormat);
            ImageReader reader = readers.next();
            StringBuilder result = new StringBuilder();
            try (ImageInputStream iis = ImageIO.createImageInputStream(tiffFile)) {
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
            RenderedImage renderedImage = oimage.getRenderedImage();
            setImage(renderedImage, rect);
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
            log.warn(ioe.getMessage(), ioe);
        }

        return text;
    }

    private static Path ocrCachesRoot(PathsUtils pathsUtils) {
        return pathsUtils.getCachesRoot().resolve("cache");
    }

    @SneakyThrows
    private Path getSubdirectoryPath(Path pdfFile) {
        String fileName = pdfFile.toFile().getName();
        int i = fileName.indexOf('.');
        String dirName = fileName.substring(0, i);
        Path newRoot = ocrCachesRoot(pathsUtils).resolve(dirName);
        if (!Files.exists(newRoot)) {
            Files.createDirectories(newRoot);
        }
        return newRoot;
    }
}
