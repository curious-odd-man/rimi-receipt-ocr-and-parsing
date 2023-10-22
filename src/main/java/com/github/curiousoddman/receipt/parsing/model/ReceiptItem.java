package com.github.curiousoddman.receipt.parsing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReceiptItem {
    private String        description;
    private String        category;
    private ReceiptNumber count;
    private ReceiptNumber pricePerUnit;
    private String        units;
    private ReceiptNumber discount;
    private ReceiptNumber finalCost;
}
