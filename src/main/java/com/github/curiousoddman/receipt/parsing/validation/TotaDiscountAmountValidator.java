package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class TotaDiscountAmountValidator extends ReceiptValidatorBase {
    @Override
    protected List<Object> performValidation(Receipt receipt) {
        BigDecimal totalSavings = receipt.getTotalSavings();
        BigDecimal itemsTotalDiscounts = BigDecimal.ZERO;
        for (ReceiptItem item : receipt.getItems()) {
            itemsTotalDiscounts = itemsTotalDiscounts.add(item.getDiscount());
        }

        if (totalSavings.compareTo(itemsTotalDiscounts) == 0) {
            return List.of();
        }

        return List.of(
                String.format("Total savings %s not equal to sum of item discounts %s", totalSavings, itemsTotalDiscounts)
        );
    }
}
