package com.github.curiousoddman.receipt.parsing;

import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        OpenCV.loadLocally();
        SpringApplication.run(Main.class, args);
    }
}
