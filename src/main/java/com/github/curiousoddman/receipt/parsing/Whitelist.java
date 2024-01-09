package com.github.curiousoddman.receipt.parsing;


import com.github.curiousoddman.receipt.parsing.utils.PathsUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Component
public class Whitelist {

    private final Set<String> whiltelist;

    @SneakyThrows
    public Whitelist() {
        whiltelist = Files
                .readAllLines(PathsUtils.WHITELIST_CONFIG_PATH)
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
