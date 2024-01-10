package com.github.curiousoddman.receipt.parsing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@EnableConfigurationProperties
@ConfigurationProperties("config.paths")
public class PathsConfig {
    private String cachesRoot;
    private String inputDir;
}
