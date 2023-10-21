package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class TotalAmountValidator implements ReceiptValidator {
    @Override
    public ValidationResult validate(Receipt receipt) {
        BigDecimal totalPayment = receipt.getTotalPayment();
        BigDecimal itemPriceSum = BigDecimal.ZERO;
        for (ReceiptItem item : receipt.getItems()) {
            itemPriceSum = itemPriceSum.add(item.getFinalCost());
        }

        if (totalPayment.compareTo(itemPriceSum) == 0) {
            return new ValidationResult(getClass());
        }

        return new ValidationResult(getClass(), List.of(
                String.format("Total amount %s not equal to sum of item prices %s", totalPayment, itemPriceSum)
        ));
    }
}
