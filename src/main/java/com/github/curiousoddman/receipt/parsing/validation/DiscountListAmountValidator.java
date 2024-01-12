package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class DiscountListAmountValidator implements ReceiptValidator {
    @Override
    public ValidationResult validate(Receipt receipt) {
        Map<String, MyBigDecimal> discounts = receipt.getDiscounts();
        MyBigDecimal totalSavings = receipt.getTotalSavings();
        if (totalSavings.isError()) {
            return ValidationResult.failure(getClass(), totalSavings.errorText());
        }
        BigDecimal totalSavingsValue = totalSavings.value();
        BigDecimal listTotalDisctount = BigDecimal.ZERO;

        for (Map.Entry<String, MyBigDecimal> discount : discounts.entrySet()) {
            MyBigDecimal amount = discount.getValue();
            if (amount.isError()) {
                return ValidationResult.failure(getClass(), amount.errorText());
            }
            BigDecimal discountAmount = amount.value();
            listTotalDisctount = listTotalDisctount.add(discountAmount);
        }

        if (totalSavingsValue.compareTo(listTotalDisctount.abs()) == 0) {
            return ValidationResult.success(getClass());
        }

        return ValidationResult.failure(
                getClass(),
                String.format("Total savings %s not equal to sum of discounts %s", totalSavingsValue, listTotalDisctount)
        );
    }
}
