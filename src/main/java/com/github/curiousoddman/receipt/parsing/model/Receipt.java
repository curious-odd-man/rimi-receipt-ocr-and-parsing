package com.github.curiousoddman.receipt.parsing.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Receipt {
    private String        fileName;
    private String        shopBrand;
    private String        shopName;
    private String        cashRegisterNumber;
    private BigDecimal    totalSavings;
    private BigDecimal    totalPayment;
    private BigDecimal    totalVat;
    private BigDecimal    shopBrandMoneyAccumulated;
    private String        documentNumber;
    private LocalDateTime receiptDateTime;

    @Singular
    private List<ReceiptItem> items;

}
