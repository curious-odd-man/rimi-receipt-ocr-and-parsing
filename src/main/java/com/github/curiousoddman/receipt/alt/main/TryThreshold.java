package com.github.curiousoddman.receipt.alt.main;

import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.file.Path;

import static com.github.curiousoddman.receipt.parsing.opencv.OpenCvUtils.loadImage;
import static com.github.curiousoddman.receipt.parsing.opencv.OpenCvUtils.saveImage;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

@Slf4j
public class TryThreshold {
    public static void main(String[] args) {
        OpenCV.loadLocally();

        Path sourceTiff = Path.of("D:\\Programming\\git\\caches\\cache\\1-163972\\1-163972.pdf.tiff");

        for (int i = 10; i < 250; i+=10) {
            Path targetImage = Path.of("D:\\Programming\\git\\caches\\cache\\test\\").resolve(sourceTiff.getFileName() + "." + i + ".tiff");
            Mat sourceMat = loadImage(sourceTiff.toAbsolutePath().toString(), IMREAD_GRAYSCALE);
            //saveImage(sourceMat, imagePath + ".grayscale.tiff");
            Mat dstMat = new Mat(sourceMat.rows(), sourceMat.cols(), sourceMat.type());
            Imgproc.threshold(sourceMat,
                              dstMat,
                              i,
                              255,
                              THRESH_BINARY);
//        Imgproc.adaptiveThreshold(sourceMat,
//                                  dstMat,
//                                  255,
//                                  ADAPTIVE_THRESH_MEAN_C,
//                                  THRESH_BINARY,
//                                  15,
//                                  30);
            log.info("Saving into {}", targetImage.toAbsolutePath());
            saveImage(dstMat, targetImage.toAbsolutePath().toString());
        }
    }
}
