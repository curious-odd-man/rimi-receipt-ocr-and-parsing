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
            BigDecimal fullPrice = item.getCount().multiply(item.getPricePerUnit());
            BigDecimal discountedPrice = fullPrice.subtract(item.getDiscount()).setScale(2, RoundingMode.HALF_UP);
            if (discountedPrice.compareTo(item.getFinalCost()) != 0) {
                errors.add(
                        String.format("Items numbers are inconsistent: %s", item)
                );
            }
        }
        return new ValidationResult(getClass(), errors);
    }
}
