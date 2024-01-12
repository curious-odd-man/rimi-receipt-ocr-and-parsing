package com.github.curiousoddman.receipt.parsing;


import com.github.curiousoddman.receipt.parsing.utils.PathsUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class IgnoreList {

    private final Set<String> ignoredFiles;

    @SneakyThrows
    public IgnoreList(PathsUtils pathsUtils) {
        if (!Files.exists(pathsUtils.getIgnoreFilePath())) {
            ignoredFiles = Set.of();
            return;
        }
        ignoredFiles = Files
                .readAllLines(pathsUtils.getIgnoreFilePath())
                .stream()
                .map(line -> line.split(" +")[0])
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isIgnored(String fileName) {
        return ignoredFiles.contains(fileName);
    }
}
