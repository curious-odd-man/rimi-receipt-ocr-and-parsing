package com.github.curiousoddman.receipt.parsing.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReceiptItem {
    private String       description;
    private MyBigDecimal count;
    private MyBigDecimal pricePerUnit;
    private String       units;
    private MyBigDecimal discount;
    private MyBigDecimal finalCost;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;
}
