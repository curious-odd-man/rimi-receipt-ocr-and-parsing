package com.github.curiousoddman.receipt.parsing.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ReceiptItem {
    private String       description;
    private MyBigDecimal count;
    private MyBigDecimal pricePerUnit;
    private String       units;
    private MyBigDecimal discount;
    private MyBigDecimal finalCost;

    private boolean      isRemoved;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorMessage;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String correctionItemError;
}
