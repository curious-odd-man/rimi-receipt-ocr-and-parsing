package com.github.curiousoddman.receipt.parsing.stats;

import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.Positioned;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.function.Function;

@Slf4j
public class ParsingStatsCollector {
    private final Map<File, TsvLine>       shopNameLocations                  = new HashMap<>();
    private final Map<File, TsvLine>       cashRegisterNumberLocations        = new HashMap<>();
    private final Map<File, TsvWord>       totalSavingsLocations              = new HashMap<>();
    private final Map<File, TsvWord>       brandMoneyLocations                = new HashMap<>();
    private final Map<File, TsvLine>       documentNumberLocations            = new HashMap<>();
    private final Map<File, List<TsvWord>> itemFinalCostLocations             = new HashMap<>();
    private final Map<File, List<TsvWord>> itemFinalCostWithDiscountLocations = new HashMap<>();
    private final Map<File, List<TsvWord>> itemDiscountLocations              = new HashMap<>();
    private final Map<File, List<TsvWord>> itemCountLocations                 = new HashMap<>();
    private final Map<File, List<TsvWord>> itemPricePerUnitLocations          = new HashMap<>();
    private final Map<File, List<TsvWord>> itemUnitsLocations                 = new HashMap<>();


    public void printStats() {
        printAllStats("Shop Name Locations", shopNameLocations.values());
        printAllStats("Cash Register Number Locations", cashRegisterNumberLocations.values());
        printAllStats("Total Savings Locations", totalSavingsLocations.values());
        printAllStats("Brand Money Locations", brandMoneyLocations.values());
        printAllStats("Document number Locations", documentNumberLocations.values());

        printAllStats("Item Final Cost Locations", itemFinalCostLocations.values().stream().flatMap(Collection::stream).toList());
        printAllStats("Item Final Cost with discount Locations", itemFinalCostWithDiscountLocations.values().stream().flatMap(Collection::stream).toList());
        printAllStats("Item Discount Locations", itemDiscountLocations.values().stream().flatMap(Collection::stream).toList());
        printAllStats("Item Count Locations", itemCountLocations.values().stream().flatMap(Collection::stream).toList());
        printAllStats("Item Price per unit Locations", itemPricePerUnitLocations.values().stream().flatMap(Collection::stream).toList());
        printAllStats("Item units Locations", itemUnitsLocations.values().stream().flatMap(Collection::stream).toList());
    }

    private static void printAllStats(String header, Collection<? extends Positioned> values) {
        IntSummaryStatistics xStat = getSingleStat(values, Positioned::getX);
        IntSummaryStatistics yStat = getSingleStat(values, Positioned::getY);
        IntSummaryStatistics endXStat = getSingleStat(values, Positioned::getEndX);
        IntSummaryStatistics endYStat = getSingleStat(values, Positioned::getEndY);
        IntSummaryStatistics widthStat = getSingleStat(values, Positioned::getWidth);
        IntSummaryStatistics heightStat = getSingleStat(values, Positioned::getHeight);
        IntSummaryStatistics indexStat = getSingleStat(values, Positioned::getIndex);

        log.info("======================== {} ====================", header);
        log.info("\tIndex: {}", statsToString(indexStat));
        log.info("\tX: {}", statsToString(xStat));
        log.info("\tY: {}", statsToString(yStat));
        log.info("\tEnd X: {}", statsToString(endXStat));
        log.info("\tEnd Y: {}", statsToString(endYStat));
        log.info("\tWidth: {}", statsToString(widthStat));
        log.info("\tHeight: {}", statsToString(heightStat));
    }

    private static String statsToString(IntSummaryStatistics stat) {
        return String.format("Min=%d, Max=%d, Avg=%f", stat.getMin(), stat.getMax(), stat.getAverage());
    }

    private static IntSummaryStatistics getSingleStat(Collection<? extends Positioned> positioned,
                                                      Function<Positioned, Integer> paramExtractor) {
        return positioned
                .stream()
                .mapToInt(paramExtractor::apply)
                .summaryStatistics();
    }

    public void collectShopNameLocation(File originalFile, TsvLine tsvLine) {
        shopNameLocations.put(originalFile, tsvLine);
    }

    public void collectCashRegisterNumberLocation(File originalFile, TsvLine tsvLine) {
        cashRegisterNumberLocations.put(originalFile, tsvLine);
    }

    public void collectTotalSavings(File originalFile, TsvWord tsvWord) {
        totalSavingsLocations.put(originalFile, tsvWord);
    }

    public void collectShopBrandMoneyLocation(File originalFile, TsvWord tsvWord) {
        brandMoneyLocations.put(originalFile, tsvWord);
    }

    public void collectDocumentNumberLocation(File originalFile, TsvLine line) {
        documentNumberLocations.put(originalFile, line);
    }

    public void collectItemFinalCostLocation(File originalFile, TsvWord tsvWord) {
        itemFinalCostLocations
                .computeIfAbsent(originalFile, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemDiscountLocation(File originalFile, TsvWord tsvWord) {
        itemDiscountLocations
                .computeIfAbsent(originalFile, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemCountLocation(File originalFile, TsvWord tsvWord) {
        itemCountLocations
                .computeIfAbsent(originalFile, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectPricePerUnitLocation(File originalFile, TsvWord tsvWord) {
        itemPricePerUnitLocations
                .computeIfAbsent(originalFile, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemUnitsLocation(File originalFile, TsvWord tsvWord) {
        itemUnitsLocations
                .computeIfAbsent(originalFile, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemFinalCostWithDiscountLocation(File originalFile, TsvWord tsvWord) {
        itemFinalCostWithDiscountLocations
                .computeIfAbsent(originalFile, k -> new ArrayList<>())
                .add(tsvWord);
    }
}
