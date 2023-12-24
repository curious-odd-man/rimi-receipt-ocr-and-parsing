package com.github.curiousoddman.receipt.parsing.model;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Receipt {
    private String                    fileName;
    private String                    shopBrand;
    private String                    shopName;
    private String                    cashRegisterNumber;
    private MyBigDecimal              totalSavings;
    private MyBigDecimal              totalPayment;
    private MyBigDecimal              usedShopBrandMoney;
    private MyBigDecimal              depositCouponPayment;
    private MyBigDecimal              shopBrandMoneyAccumulated;
    private String                    documentNumber;
    private MyLocalDateTime           receiptDateTime;
    private Map<String, MyBigDecimal> discounts;

    @Singular
    private List<ReceiptItem> items;

}
