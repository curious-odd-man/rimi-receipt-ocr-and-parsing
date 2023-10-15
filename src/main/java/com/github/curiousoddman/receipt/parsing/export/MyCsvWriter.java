package com.github.curiousoddman.receipt.parsing.export;

import lombok.experimental.UtilityClass;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@UtilityClass
public class MyCsvWriter {
    public static <T> void writeCsv(List<T> pojos, Map<String, Function<T, String>> fieldExtractors) {
        CSVPrinter csvPrinter = null;
        FileWriter fileWriter = null;
        CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
        try {
            fileWriter = new FileWriter("export.csv", StandardCharsets.UTF_8);
            csvPrinter = new CSVPrinter(fileWriter, csvFormat);
            csvPrinter.printRecord(fieldExtractors.keySet());
            for (T pojo : pojos) {
                csvPrinter.printRecord(fieldExtractors.values()
                                                      .stream()
                                                      .map(extractor -> extractor.apply(pojo))
                                                      .collect(Collectors.toList()));
            }
        } catch (Exception e) {
            System.out.println("CSV writing error");
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
                csvPrinter.close();
            } catch (IOException e) {
                System.out.println("closing error");
                e.printStackTrace();
            }
        }
    }
}
