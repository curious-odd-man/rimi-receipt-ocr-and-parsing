package com.github.curiousoddman.receipt.parsing.opencv;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.highgui.HighGui.toBufferedImage;
import static org.opencv.imgcodecs.Imgcodecs.IMREAD_GRAYSCALE;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;

@Slf4j
public class OpenCvUtils {

    public static final int BLANK_PIXELS_BETWEEN_ROWS = 30;

    public static Mat loadImage(String imagePath, int config) {
        return Imgcodecs.imread(imagePath, config);
    }

    public static void saveImage(Mat imageMatrix, String targetPath) {
        Imgcodecs.imwrite(targetPath, imageMatrix);
    }

    @SneakyThrows
    public static void doImagePreprocessing(Path sourceImage, Path targetImage) {
        Mat sourceMat = loadImage(sourceImage.toAbsolutePath().toString(), IMREAD_GRAYSCALE);
        //saveImage(sourceMat, imagePath + ".grayscale.tiff");
        Mat dstMat = new Mat(sourceMat.rows(), sourceMat.cols(), sourceMat.type());
        Imgproc.threshold(sourceMat,
                          dstMat,
                          190,      // 180
                          255,
                          THRESH_BINARY);       // THRESH_BINARY

        //saveImage(dstMat, targetImage.toAbsolutePath().toString());
        //BufferedImage bufferedImage = ImageIO.read(targetImage.toFile());
        BufferedImage bufferedImage = (BufferedImage) toBufferedImage(dstMat);
        BufferedImage transformedImage = addSpaceBetweenLines(bufferedImage, targetImage);
        ImageIO.write(transformedImage, "tiff", targetImage.toFile());
    }

    @SneakyThrows
    private static BufferedImage addSpaceBetweenLines(BufferedImage image, Path targetImage) {
        int height = image.getHeight();
        int width = image.getWidth();
        List<Line> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        int lineRowFrom = 0;
        boolean isLineStarted = false;
        for (int row = 0; row < height; row++) {
            int[] rowData = new int[width];
            BlackWhitePixelStats blackWhitePixelRowStats = getBlackWhitePixelRowStats(image.getRGB(0, row, width, 1, rowData, 0, 0));
            if (blackWhitePixelRowStats.isAllWhite()) {
                if (isLineStarted) {
                    lines.add(new Line(lineRowFrom, row - 1));
                    isLineStarted = false;
                }
                lineRowFrom = row + 1;
            } else {
                isLineStarted = true;
                sb.append(row).append(": ").append(blackWhitePixelRowStats).append('\n');
            }
        }

        Files.writeString(targetImage.getParent().resolve("pixel_colors.txt"), sb.toString());
        int additionalHeight = lines.size() * BLANK_PIXELS_BETWEEN_ROWS;
        BufferedImage newImage = new BufferedImage(width, height + additionalHeight, image.getType());
        Graphics2D g2d = newImage.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, width, height + additionalHeight);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            drawImage(image, line, width, g2d, i, newImage);
        }
        g2d.dispose();
        return newImage;
    }

    private static void drawImage(BufferedImage image, Line line, int width, Graphics2D g2d, int i, BufferedImage newImage) throws IOException {
        BufferedImage subimage = image.getSubimage(0, line.yFrom(), width, line.height());
        g2d.drawImage(subimage, 0, line.yFrom() + i * BLANK_PIXELS_BETWEEN_ROWS, width, line.height(), null);
    }

    private record Line(int yFrom, int yTo) {

        public int height() {
            return yTo - yFrom + 1;
        }
    }

    private static BlackWhitePixelStats getBlackWhitePixelRowStats(int[] pixelsColor) {
        BlackWhitePixelStats stats = new BlackWhitePixelStats();
        boolean isPreviousPixelBlack = false;
        for (int i = 0; i < pixelsColor.length; i++) {
            int pixelColor = pixelsColor[i];
            boolean isPixelBlack = isBlackPixel(pixelColor);
            if (isPreviousPixelBlack != isPixelBlack) {
                isPreviousPixelBlack = isPixelBlack;
                stats.getChangeLocation().add(i);
            }
            if (isPixelBlack) {
                stats.incBlack();
            } else {
                stats.incWhite();
            }
        }
        return stats;
    }

    private static boolean isBlackPixel(int pixel) {
        return (pixel & 0x00FFFFFF) == 0;
    }

    @Data
    private static class BlackWhitePixelStats {
        private int           blackCount;
        private int           whiteCount;
        private List<Integer> changeLocation = new ArrayList<>();

        public void incBlack() {
            ++blackCount;
        }

        public void incWhite() {
            ++whiteCount;
        }

        public boolean isAllWhite() {
            return blackCount == 0 && whiteCount > 0;
        }

        @Override
        public String toString() {
            return '{' +
                    "black=" + blackCount +
                    ", white=" + whiteCount +
                    ", changes=" + changeLocation.size() +
                    ", chgLocs=" + changeLocation +
                    '}';
        }
    }
}
