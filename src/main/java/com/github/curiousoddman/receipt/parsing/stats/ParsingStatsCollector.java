package com.github.curiousoddman.receipt.parsing.stats;

import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.Positioned;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

@Slf4j
public class ParsingStatsCollector {
    private final Map<Path, TsvLine>       shopNameLocations                  = new HashMap<>();
    private final Map<Path, TsvLine>       cashRegisterNumberLocations        = new HashMap<>();
    private final Map<Path, TsvWord>       totalSavingsLocations              = new HashMap<>();
    private final Map<Path, TsvWord>       brandMoneyLocations                = new HashMap<>();
    private final Map<Path, TsvLine>       documentNumberLocations            = new HashMap<>();
    private final Map<Path, List<TsvWord>> itemFinalCostLocations             = new HashMap<>();
    private final Map<Path, List<TsvWord>> itemFinalCostWithDiscountLocations = new HashMap<>();
    private final Map<Path, List<TsvWord>> itemDiscountLocations              = new HashMap<>();
    private final Map<Path, List<TsvWord>> itemCountLocations                 = new HashMap<>();
    private final Map<Path, List<TsvWord>> itemPricePerUnitLocations          = new HashMap<>();
    private final Map<Path, List<TsvWord>> itemUnitsLocations                 = new HashMap<>();


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
        log.info("\tEnd X: {}", statsToString(endXStat));
        log.info("\tWidth: {}", statsToString(widthStat));

        log.info("\tY: {}", statsToString(yStat));
        log.info("\tEnd Y: {}", statsToString(endYStat));
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

    public void collectShopNameLocation(Path originalPath, TsvLine tsvLine) {
        shopNameLocations.put(originalPath, tsvLine);
    }

    public void collectCashRegisterNumberLocation(Path originalPath, TsvLine tsvLine) {
        cashRegisterNumberLocations.put(originalPath, tsvLine);
    }

    public void collectTotalSavings(Path originalPath, TsvWord tsvWord) {
        totalSavingsLocations.put(originalPath, tsvWord);
    }

    public void collectShopBrandMoneyLocation(Path originalPath, TsvWord tsvWord) {
        brandMoneyLocations.put(originalPath, tsvWord);
    }

    public void collectDocumentNumberLocation(Path originalPath, TsvLine line) {
        documentNumberLocations.put(originalPath, line);
    }

    public void collectItemFinalCostLocation(Path originalPath, TsvWord tsvWord) {
        itemFinalCostLocations
                .computeIfAbsent(originalPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemDiscountLocation(Path originalPath, TsvWord tsvWord) {
        itemDiscountLocations
                .computeIfAbsent(originalPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemCountLocation(Path originalPath, TsvWord tsvWord) {
        itemCountLocations
                .computeIfAbsent(originalPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectPricePerUnitLocation(Path originalPath, TsvWord tsvWord) {
        itemPricePerUnitLocations
                .computeIfAbsent(originalPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemUnitsLocation(Path originalPath, TsvWord tsvWord) {
        itemUnitsLocations
                .computeIfAbsent(originalPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemFinalCostWithDiscountLocation(Path originalPath, TsvWord tsvWord) {
        itemFinalCostWithDiscountLocations
                .computeIfAbsent(originalPath, k -> new ArrayList<>())
                .add(tsvWord);
    }
}
