package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Discount;
import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DiscountListAmountValidator implements ReceiptValidator {
    @Override
    public ValidationResult validate(Receipt receipt) {
        List<Discount> discountsList = receipt.getDiscounts();
        MyBigDecimal discounts = receipt.getTotalSavings();
        if (discounts.isError()) {
            return new ValidationResult(getClass(), List.of(
                    String.format(discounts.errorText())
            ));
        }
        BigDecimal totalSavings = discounts.value();
        BigDecimal listTotalDisctount = BigDecimal.ZERO;

        for (Discount discount : discountsList) {
            MyBigDecimal amount = discount.getAmount();
            if (amount.isError()) {
                if (amount.isError()) {
                    return new ValidationResult(getClass(), List.of(
                            String.format(amount.errorText())
                    ));
                }
            }
            BigDecimal discountAmount = amount.value();
            listTotalDisctount = listTotalDisctount.add(discountAmount);
        }

        if (totalSavings.compareTo(listTotalDisctount.abs()) == 0) {
            return new ValidationResult(getClass());
        }

        return new ValidationResult(getClass(), List.of(
                String.format("Total savings %s not equal to sum of item discounts %s", totalSavings, listTotalDisctount)
        ));
    }
}
