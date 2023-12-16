package com.github.curiousoddman.receipt.parsing.opencv;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.file.Path;

import static org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_TRUNC;

public class OpenCvUtils {

    public static Mat loadImage(String imagePath, int config) {
        return Imgcodecs.imread(imagePath, config);
    }

    public static void saveImage(Mat imageMatrix, String targetPath) {
        Imgcodecs.imwrite(targetPath, imageMatrix);
    }

    public static void doImagePreprocessing(Path sourceImage, Path targetImage) {
        Mat sourceMat = loadImage(sourceImage.toAbsolutePath().toString(), IMREAD_GRAYSCALE);
        //saveImage(sourceMat, imagePath + ".grayscale.tiff");
        Mat dstMat = new Mat(sourceMat.rows(), sourceMat.cols(), sourceMat.type());
        Imgproc.threshold(sourceMat,
                          dstMat,
                          190,      // 180
                          255,
                          THRESH_BINARY);       // THRESH_BINARY
        saveImage(dstMat, targetImage.toAbsolutePath().toString());
    }
}
