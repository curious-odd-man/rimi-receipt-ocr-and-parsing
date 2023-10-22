package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class ItemNumbersValidator implements ReceiptValidator {
    @Override
    public ValidationResult validate(Receipt receipt) {
        List<Object> errors = new ArrayList<>();
        for (ReceiptItem item : receipt.getItems()) {
            if (!isItemValid(item)) {
                errors.add(
                        String.format("Items numbers are inconsistent: %s", item)
                );
            }
        }
        return new ValidationResult(getClass(), errors);
    }

    public boolean isItemValid(ReceiptItem item) {
        BigDecimal fullPrice = item.getCount().value().multiply(item.getPricePerUnit().value());
        BigDecimal discountedPrice = fullPrice.subtract(item.getDiscount().value()).setScale(2, RoundingMode.HALF_UP);
        return discountedPrice.compareTo(item.getFinalCost().value()) == 0;
    }
}
