package com.github.curiousoddman.receipt.parsing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("debug")
public class DebugConfig {
    private boolean saveReOcrAreaImages;
}
