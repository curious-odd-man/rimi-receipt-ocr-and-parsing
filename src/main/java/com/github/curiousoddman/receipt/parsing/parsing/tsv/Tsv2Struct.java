package com.github.curiousoddman.receipt.parsing.parsing.tsv;

import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.*;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.raw.TsvRow;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Tsv2Struct {

    @SneakyThrows
    public TsvDocument parseTsv(String tsvContents) {
        TsvDocument document = new TsvDocument(new ArrayList<>());

        List<String> lines = tsvContents.lines().toList();

        List<TsvRow> tsvRows = lines
                .stream()
                .map(Tsv2Struct::lineToTsvRow)
                .toList();

        Map<Integer, List<TsvRow>> rowsPerPage = tsvRows
                .stream()
                .collect(Collectors.groupingBy(TsvRow::pageNum));

        for (List<TsvRow> pageRows : rowsPerPage.values()) {
            pageRows = new ArrayList<>(pageRows);
            TsvRow pageIdRow = findOnlyOneAndRemove(1, pageRows);
            TsvPage newPage = new TsvPage(document,
                                          pageIdRow.pageNum(),
                                          pageIdRow.left(),
                                          pageIdRow.top(),
                                          pageIdRow.width(),
                                          pageIdRow.height(),
                                          new ArrayList<>());
            document.getPages().add(newPage);

            for (List<TsvRow> blockRows : pageRows.stream().collect(Collectors.groupingBy(TsvRow::blockNum)).values()) {
                blockRows = new ArrayList<>(blockRows);
                TsvRow blockIdRow = findOnlyOneAndRemove(2, blockRows);
                TsvBlock newBlock = new TsvBlock(
                        newPage,
                        blockIdRow.blockNum(),
                        blockIdRow.left(),
                        blockIdRow.top(),
                        blockIdRow.width(),
                        blockIdRow.height(),
                        new ArrayList<>()
                );
                newPage.getBlocks().add(newBlock);

                for (List<TsvRow> paragraphRows : blockRows.stream().collect(Collectors.groupingBy(TsvRow::paragraphNum)).values()) {
                    paragraphRows = new ArrayList<>(paragraphRows);
                    TsvRow paragraphIdRow = findOnlyOneAndRemove(3, paragraphRows);
                    TsvParagraph newParagraph = new TsvParagraph(
                            newBlock,
                            paragraphIdRow.paragraphNum(),
                            paragraphIdRow.left(),
                            paragraphIdRow.top(),
                            paragraphIdRow.width(),
                            paragraphIdRow.height(),
                            new ArrayList<>()
                    );
                    newBlock.getParagraphs().add(newParagraph);

                    for (List<TsvRow> lineRows : paragraphRows.stream().collect(Collectors.groupingBy(TsvRow::lineNum)).values()) {
                        lineRows = new ArrayList<>(lineRows);
                        TsvRow lineIdRow = findOnlyOneAndRemove(4, lineRows);
                        TsvLine newLine = new TsvLine(
                                newParagraph,
                                lineIdRow.lineNum(),
                                lineIdRow.left(),
                                lineIdRow.top(),
                                lineIdRow.width(),
                                lineIdRow.height(),
                                new ArrayList<>()
                        );

                        lineRows
                                .stream()
                                .map(lineRow -> new TsvWord(newLine,
                                                            lineRow.wordNum(),
                                                            lineRow.left(),
                                                            lineRow.top(),
                                                            lineRow.width(),
                                                            lineRow.height(),
                                                            lineRow.confidence(),
                                                            lineRow.text())).
                                forEach(newLine.getWords()::add);
                        newParagraph.getLines().add(newLine);
                    }
                }
            }
        }

        return document;
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