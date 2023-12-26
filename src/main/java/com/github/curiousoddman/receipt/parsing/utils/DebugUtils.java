package com.github.curiousoddman.receipt.parsing.utils;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@UtilityClass
public class DebugUtils {
    private static void showImage(BufferedImage subimage) {
        JLabel picLabel = new JLabel(new ImageIcon(subimage));

        JPanel jPanel = new JPanel();
        jPanel.add(picLabel);

        JFrame f = new JFrame();
        f.setSize(new Dimension(subimage.getWidth(), subimage.getHeight()));
        f.add(jPanel);
        f.setVisible(true);
    }
}
