package com.github.curiousoddman.receipt.parsing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReceiptItem {
    private String     description;
    private String     category;
    private BigDecimal count;
    private BigDecimal pricePerUnit;
    private String     units;
    private BigDecimal discount;
    private BigDecimal finalCost;
}
