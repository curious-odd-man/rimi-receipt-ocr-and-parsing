package com.github.curiousoddman.receipt.parsing.utils;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {
    private static final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();

    public static final JsonMapper OBJECT_MAPPER = JsonMapper.builder()
                                                             .addModule(new ParameterNamesModule())
                                                             .addModule(new Jdk8Module())
                                                             .addModule(new JavaTimeModule())
                                                             .build();

    static {
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    }

    public static final ObjectWriter OBJECT_WRITER = OBJECT_MAPPER.writer(prettyPrinter);
}
