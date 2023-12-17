package com.github.curiousoddman.receipt.parsing.model;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Receipt {
    private String          fileName;
    private String          shopBrand;
    private String          shopName;
    private String          cashRegisterNumber;
    private MyBigDecimal    totalSavings;
    private MyBigDecimal    totalPayment;
    private MyBigDecimal    usedShopBrandMoney;
    private MyBigDecimal    depositCouponPayment;
    //    private MyBigDecimal totalVat;
    private MyBigDecimal    shopBrandMoneyAccumulated;
    private String          documentNumber;
    private MyLocalDateTime receiptDateTime;
    private List<Discount>  discounts;

    @Singular
    private List<ReceiptItem> items;

}
