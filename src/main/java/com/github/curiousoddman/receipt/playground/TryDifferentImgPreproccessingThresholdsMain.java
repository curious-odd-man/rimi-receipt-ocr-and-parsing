package com.github.curiousoddman.receipt.playground;

import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.nio.file.Path;

import static com.github.curiousoddman.receipt.parsing.utils.ImageUtils.loadImage;
import static com.github.curiousoddman.receipt.parsing.utils.ImageUtils.saveImage;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE;
import static org.opencv.imgproc.Imgproc.*;

@Slf4j
public class TryDifferentImgPreproccessingThresholdsMain {
    public static void main(String[] args) {
        OpenCV.loadLocally();

        Path sourceTiff = Path.of("W:\\Programming\\git\\caches\\cache\\3-311743\\3-311743.pdf.tiff");

        for (int i = 180; i < 200; i+=2) {
            Path targetImage = Path.of("W:\\Programming\\git\\caches\\cache\\test\\").resolve(sourceTiff.getFileName() + "." + i + ".tiff");
            Mat sourceMat = loadImage(sourceTiff.toAbsolutePath().toString(), IMREAD_GRAYSCALE);
            //saveImage(sourceMat, imagePath + ".grayscale.tiff");
            Mat dstMat = new Mat(sourceMat.rows(), sourceMat.cols(), sourceMat.type());
            Imgproc.threshold(sourceMat,
                              dstMat,
                              i,
                              255,
                              THRESH_BINARY);
//        Imgproc.adaptiveThreshold(dstMat,
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
