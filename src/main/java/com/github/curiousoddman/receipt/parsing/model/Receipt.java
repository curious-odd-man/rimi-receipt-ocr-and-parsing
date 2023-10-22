package com.github.curiousoddman.receipt.parsing.model;

import lombok.*;

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
    private String       cashRegisterNumber;
    private MyBigDecimal totalSavings;
    private MyBigDecimal totalPayment;
    private MyBigDecimal totalVat;
    private MyBigDecimal shopBrandMoneyAccumulated;
    private String       documentNumber;
    private LocalDateTime receiptDateTime;

    @Singular
    private List<ReceiptItem> items;

}
