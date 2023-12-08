package com.github.curiousoddman.receipt.parsing.parsing.tsv;

import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.*;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Tsv2Struct {

    @SneakyThrows
    public TsvDocument parseTsv(String tsvContents) {
        List<String> lines = tsvContents.lines().toList();

        List<TsvRow> tsvRows = lines
                .stream()
                .map(Tsv2Struct::lineToTsvRow)
                .toList();

        Map<TsvContainerKey, List<TsvRow>> collectedByKey = tsvRows
                .stream()
                .collect(Collectors.groupingBy(TsvRow::getContainerKey));

        List<TsvContainer> tsvContainers = collectedByKey
                .entrySet()
                .stream()
                .map(this::transformToContainer)
                .toList();

        return new TsvDocument(tsvContainers);
    }

    private TsvContainer transformToContainer(Map.Entry<TsvContainerKey, List<TsvRow>> entry) {
        TsvContainerKey key = entry.getKey();
        List<TsvRow> rows = entry.getValue();

        TsvRow pageRow = findOnlyOneAndRemove(1, rows);
        TsvRow blockRow = findOnlyOneAndRemove(2, rows);
        TsvRow paragraphRow = findOnlyOneAndRemove(3, rows);

        List<TsvLine> lines = rowsToLines(rows);

        return new TsvContainer(
                key,
                pageRow,
                blockRow,
                paragraphRow,
                lines
        );
    }

    private static List<TsvLine> rowsToLines(List<TsvRow> rows) {
        List<TsvLine> lines = new ArrayList<>();
        Map<Integer, List<TsvRow>> rowsByLine = rows
                .stream()
                .collect(Collectors.groupingBy(TsvRow::lineNum));

        for (Map.Entry<Integer, List<TsvRow>> rowNumToLines : rowsByLine.entrySet()) {
            Integer rowIndex = rowNumToLines.getKey();
            List<TsvRow> tsvRows = rowNumToLines.getValue();
            TsvRow lineTsvRow = findOnlyOneAndRemove(4, tsvRows);

            List<TsvWord> words = tsvRows
                    .stream()
                    .map(TsvWord::new)
                    .sorted(Comparator.comparingInt(TsvWord::getWordNumber))
                    .toList();

            lines.add(new TsvLine(
                    lineTsvRow,
                    words
            ));
        }

        lines.sort(Comparator.comparingInt(TsvLine::lineNum));
        return lines;
    }

    private static TsvRow findOnlyOneAndRemove(int rowType, List<TsvRow> rows) {
        TsvRow found = null;
        Iterator<TsvRow> iterator = rows.iterator();
        while (iterator.hasNext()) {
            var row = iterator.next();
            if (row.level() == rowType) {
                if (found == null) {
                    found = row;
                    iterator.remove();
                } else {
                    throw new RuntimeException("Duplicate row type found!");
                }
            }
        }

        return found;
    }

    private static TsvRow lineToTsvRow(String line) {
        String[] split = line.split("\t");
        return new TsvRow(
                Integer.parseInt(split[0]),
                Integer.parseInt(split[1]),
                Integer.parseInt(split[2]),
                Integer.parseInt(split[3]),
                Integer.parseInt(split[4]),
                Integer.parseInt(split[5]),
                Integer.parseInt(split[6]),
                Integer.parseInt(split[7]),
                Integer.parseInt(split[8]),
                Integer.parseInt(split[9]),
                new BigDecimal(split[10]),
                split.length == 12 ? split[11] : null
        );
    }
}
