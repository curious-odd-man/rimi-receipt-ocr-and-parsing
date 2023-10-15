package com.github.curiousoddman.receipt.parsing.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Receipt {
    private String     shopBrand;
    private String     shopName;
    private String     cashRegisterNumber;
    private BigDecimal totalSavings;
    private BigDecimal totalPayment;
    private BigDecimal totalVat;
    private BigDecimal shopBrandMoneyAccumulated;
    private String     documentNumber;

    @Singular
    private List<ReceiptItem> items;

}
