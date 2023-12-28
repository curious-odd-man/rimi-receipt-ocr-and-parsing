package com.github.curiousoddman.receipt.parsing.stats;

import com.github.curiousoddman.receipt.parsing.model.Receipt;

public interface ReceiptStatsCollector {
    void printSummary();

    void collect(Receipt receipt);
}
