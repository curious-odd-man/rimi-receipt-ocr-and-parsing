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
    private String        cashRegisterNumber;
    private ReceiptNumber totalSavings;
    private ReceiptNumber totalPayment;
    private ReceiptNumber totalVat;
    private ReceiptNumber shopBrandMoneyAccumulated;
    private String        documentNumber;
    private LocalDateTime receiptDateTime;

    @Singular
    private List<ReceiptItem> items;

}
