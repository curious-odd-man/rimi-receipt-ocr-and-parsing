package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class TotalDiscountAmountValidator implements ReceiptValidator {
    @Override
    public ValidationResult validate(Receipt receipt) {
        MyBigDecimal receiptTotalSavings = receipt.getTotalSavings();
        if (receiptTotalSavings.isError()) {
            return new ValidationResult(getClass(), List.of(
                    String.format(receiptTotalSavings.errorText())
            ));
        }
        BigDecimal totalSavings = receiptTotalSavings.value();
        BigDecimal itemsTotalDiscounts = BigDecimal.ZERO;
        for (ReceiptItem item : receipt.getItems()) {
            MyBigDecimal discount = item.getDiscount();
            if (discount.isError()) {
                return new ValidationResult(getClass(), List.of(
                        String.format(discount.errorText())
                ));
            }
            itemsTotalDiscounts = itemsTotalDiscounts.add(discount.value());
        }

        if (totalSavings.compareTo(itemsTotalDiscounts.abs()) == 0) {
            return new ValidationResult(getClass());
        }

        return new ValidationResult(getClass(), List.of(
                String.format("Total savings %s not equal to sum of item discounts %s", totalSavings, itemsTotalDiscounts)
        ));
    }
}
