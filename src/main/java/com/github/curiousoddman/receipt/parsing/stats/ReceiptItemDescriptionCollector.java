package com.github.curiousoddman.receipt.parsing.stats;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@Component
public class ReceiptItemDescriptionCollector implements ReceiptStatsCollector {
    private final Set<String> descriptions = new ConcurrentSkipListSet<>();

    @Override
    public void printSummary() {
        log.info("List of all unique descriptions");
        for (String description : descriptions) {
            log.info("\t{}", description);
        }
    }

    @Override
    public void collect(Receipt receipt) {
        List<String> list = receipt.getItems().stream().map(ReceiptItem::getDescription).toList();
        descriptions.addAll(list);
    }
}
