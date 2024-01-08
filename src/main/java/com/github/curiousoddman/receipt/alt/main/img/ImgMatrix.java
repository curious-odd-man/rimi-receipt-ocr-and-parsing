package com.github.curiousoddman.receipt.alt.main.img;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static com.github.curiousoddman.receipt.parsing.utils.ImageUtils.isBlackPixel;


@RequiredArgsConstructor
public class ImgMatrix {
    @Getter
    private final int         width;
    @Getter
    private final int         height;
    private final boolean[][] mat;

    public ImgMatrix(BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();
        mat = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                mat[y][x] = isBlackPixel(image, x, y);
            }
        }
    }

    public List<Point> compareToOtherAt(ImgMatrix other, int x, int y) {
        List<Point> diff = new ArrayList<>();
        int searchWidth;
        int searchHeight;
        int widthDifference = width - other.width;
        int heightDifference = height - other.height;
        if (widthDifference < 0 || heightDifference < 0) {
            for (int row = 0; row < other.height; row++) {
                for (int col = 0; col < other.height; col++) {
                    if (row >= height || col >= width) {
                        diff.add(new Point(col, row));
                    }
                }
            }
            searchHeight = 1;
            searchWidth = 1;
        } else {
            searchHeight = heightDifference;
            searchWidth = widthDifference;
        }
        for (int row = -10; row < searchHeight + 10; row++) {
            int thisRow = y + row;
            if (areBothNonExistentRows(row, thisRow, other.height)) {
                continue;
            }
            for (int col = -10; col < searchWidth + 10; col++) {
                int thisCol = x + col;
                if (areBothNonExistentColumns(col, thisCol, other.width)) {
                    continue;
                }
                if (isBlack(thisCol, thisRow) != other.isBlack(col, row)) {
                    diff.add(new Point(col, row));
                }
            }
        }
        return diff;
    }

    private boolean areBothNonExistentColumns(int col, int thisCol, int otherWidth) {
        return col < 0 && thisCol < 0 || col > otherWidth && thisCol > width;
    }

    private boolean areBothNonExistentRows(int row, int thisRow, int otherHeight) {
        return row < 0 && thisRow < 0 || row > otherHeight && thisRow > height;
    }

    public ImgMatrix subimage(int x, int y, int w, int h) {
        int copyWidth = Math.min(w, width - x);
        int copyHeight = Math.min(h, height - y);
        boolean[][] newMat = new boolean[copyHeight][copyWidth];
        for (int row = 0; row < newMat.length; row++) {
            boolean[] target = newMat[row];
            boolean[] source = mat[y + row];
            System.arraycopy(source, x, target, 0, copyWidth);
        }
        return new ImgMatrix(
                w,
                h,
                newMat
        );
    }

    private boolean isBlack(int x, int y) {
        if (isOutOfBounds(x, y)) {
            return false;
        }
        return mat[y][x];
    }

    private boolean isOutOfBounds(int x, int y) {
        return x < 0 || y < 0 || x >= width || y >= height;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBlack(x, y)) {
                    sb.append('#');
                } else {
                    sb.append('.');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
