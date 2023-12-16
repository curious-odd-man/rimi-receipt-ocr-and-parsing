package com.github.curiousoddman.receipt.parsing;


import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Whitelist {

    private final Set<String> whiltelist;

    @SneakyThrows
    public Whitelist() {
        Path ignoreListPath = Path.of("D:\\Programming\\git\\private-tools\\receipts-parsing\\data\\whitelist.txt");
        whiltelist = Files
                .readAllLines(ignoreListPath)
                .stream()
                .map(line -> line.split(" +")[0])
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isWhitelisted(String fileName) {
        if (whiltelist.isEmpty()) {
            return true;
        }
        return whiltelist.contains(fileName);
    }
}
