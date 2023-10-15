package com.github.curiousoddman.receipt.parsing;


import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IgnoreList {

    private final Set<String> ignoredFiles;

    @SneakyThrows
    public IgnoreList() {
        Path ignoreListPath = Path.of("D:\\Programming\\git\\private-tools\\receipts-parsing\\data\\ignore.txt");
        ignoredFiles = Files
                .readAllLines(ignoreListPath)
                .stream()
                .map(line -> line.split(" +")[0])
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isIgnored(String fileName) {
        return ignoredFiles.contains(fileName);
    }
}
