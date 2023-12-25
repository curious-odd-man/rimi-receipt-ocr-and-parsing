package com.github.curiousoddman.receipt.parsing.utils;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {
    public static final JsonMapper   OBJECT_MAPPER = JsonMapper.builder()
                                                               .addModule(new ParameterNamesModule())
                                                               .addModule(new Jdk8Module())
                                                               .addModule(new JavaTimeModule())
                                                               .build();
    public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER
            .writerWithDefaultPrettyPrinter();
}
