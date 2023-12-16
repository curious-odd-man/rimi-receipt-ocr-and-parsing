package com.github.curiousoddman.receipt.parsing.stats;

import com.github.curiousoddman.receipt.parsing.model.OriginFile;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.Positioned;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvLine;
import com.github.curiousoddman.receipt.parsing.parsing.tsv.structure.TsvWord;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;

@Slf4j
public class ParsingStatsCollector {
    private final Map<OriginFile, TsvLine>       shopNameLocations                  = new HashMap<>();
    private final Map<OriginFile, TsvLine>       cashRegisterNumberLocations        = new HashMap<>();
    private final Map<OriginFile, TsvWord>       totalSavingsLocations              = new HashMap<>();
    private final Map<OriginFile, TsvWord>       brandMoneyLocations                = new HashMap<>();
    private final Map<OriginFile, TsvLine>       documentNumberLocations            = new HashMap<>();
    private final Map<OriginFile, List<TsvWord>> itemFinalCostLocations             = new HashMap<>();
    private final Map<OriginFile, List<TsvWord>> itemFinalCostWithDiscountLocations = new HashMap<>();
    private final Map<OriginFile, List<TsvWord>> itemDiscountLocations              = new HashMap<>();
    private final Map<OriginFile, List<TsvWord>> itemCountLocations                 = new HashMap<>();
    private final Map<OriginFile, List<TsvWord>> itemPricePerUnitLocations          = new HashMap<>();
    private final Map<OriginFile, List<TsvWord>> itemUnitsLocations                 = new HashMap<>();


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

    public void collectShopNameLocation(OriginFile originPath, TsvLine tsvLine) {
        shopNameLocations.put(originPath, tsvLine);
    }

    public void collectCashRegisterNumberLocation(OriginFile originPath, TsvLine tsvLine) {
        cashRegisterNumberLocations.put(originPath, tsvLine);
    }

    public void collectTotalSavings(OriginFile originPath, TsvWord tsvWord) {
        totalSavingsLocations.put(originPath, tsvWord);
    }

    public void collectShopBrandMoneyLocation(OriginFile originPath, TsvWord tsvWord) {
        brandMoneyLocations.put(originPath, tsvWord);
    }

    public void collectDocumentNumberLocation(OriginFile originPath, TsvLine line) {
        documentNumberLocations.put(originPath, line);
    }

    public void collectItemFinalCostLocation(OriginFile originPath, TsvWord tsvWord) {
        itemFinalCostLocations
                .computeIfAbsent(originPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemDiscountLocation(OriginFile originPath, TsvWord tsvWord) {
        itemDiscountLocations
                .computeIfAbsent(originPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemCountLocation(OriginFile originPath, TsvWord tsvWord) {
        itemCountLocations
                .computeIfAbsent(originPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectPricePerUnitLocation(OriginFile originPath, TsvWord tsvWord) {
        itemPricePerUnitLocations
                .computeIfAbsent(originPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemUnitsLocation(OriginFile originPath, TsvWord tsvWord) {
        itemUnitsLocations
                .computeIfAbsent(originPath, k -> new ArrayList<>())
                .add(tsvWord);
    }

    public void collectItemFinalCostWithDiscountLocation(OriginFile originPath, TsvWord tsvWord) {
        itemFinalCostWithDiscountLocations
                .computeIfAbsent(originPath, k -> new ArrayList<>())
                .add(tsvWord);
    }
}
