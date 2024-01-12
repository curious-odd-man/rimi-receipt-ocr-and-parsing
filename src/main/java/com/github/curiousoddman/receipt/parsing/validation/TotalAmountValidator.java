package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.utils.Constants;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TotalAmountValidator implements ReceiptValidator {
    @Override
    public ValidationResult validate(Receipt receipt) {
        MyBigDecimal receiptTotalPayment = receipt.getTotalAmount();
        if (receiptTotalPayment.isError()) {
            return ValidationResult.failure(getClass(), receiptTotalPayment.errorText());
        }

        MyBigDecimal usedShopBrandMoney = receipt
                .getDiscounts()
                .getOrDefault(Constants.USED_SHOP_BRAND_MONEY, MyBigDecimal.zero());
        if (usedShopBrandMoney.isError()) {
            return ValidationResult.failure(getClass(), usedShopBrandMoney.errorText());
        }

        BigDecimal totalPayment = receiptTotalPayment.value()
                                                     .add(usedShopBrandMoney.value().abs());
        BigDecimal itemPriceSum = BigDecimal.ZERO;
        for (ReceiptItem item : receipt.getItems()) {
            MyBigDecimal finalCost = item.getFinalCost();
            if (finalCost.isError()) {
                return ValidationResult.failure(getClass(), finalCost.errorText());
            }
            if (!item.isRemoved()) {
                itemPriceSum = itemPriceSum.add(finalCost.value());
            }
        }

        if (totalPayment.compareTo(itemPriceSum) == 0) {
            return ValidationResult.success(getClass());
        }

        return ValidationResult.failure(
                getClass(),
                String.format("Total amount %s minus shop brand money %s not equal to sum of item prices %s",
                              totalPayment, usedShopBrandMoney.value(), itemPriceSum)
        );
    }
}
