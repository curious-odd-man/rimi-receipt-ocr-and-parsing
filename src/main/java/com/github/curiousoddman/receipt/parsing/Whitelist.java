package com.github.curiousoddman.receipt.parsing;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Component
public class Whitelist {

    private final Set<String> whiltelist;

    @SneakyThrows
    public Whitelist() {
        Path ignoreListPath = Path.of("D:\\Programming\\git\\private-tools\\receipts-parsing\\data\\whitelist.txt");
        whiltelist = Files
                .readAllLines(ignoreListPath)
                .stream()
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !line.isBlank())
                .map(line -> line.split(" +")[0])
                .collect(Collectors.toUnmodifiableSet());

        if (!whiltelist.isEmpty()) {
            log.warn("Whitelist is not empty. Running only:");
            for (String s : whiltelist) {
                log.warn("\t{}", s);
            }
            log.warn("=-----=");
        }
    }

    public boolean isWhitelisted(String fileName) {
        if (whiltelist.isEmpty()) {
            return true;
        }
        return whiltelist.contains(fileName);
    }
}
