package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
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
        List<String> errors = new ArrayList<>();
        for (ReceiptItem item : receipt.getItems()) {
            if (!isItemValid(item)) {
                errors.add(
                        String.format("Items numbers are inconsistent: %s", item)
                );
            }
        }
        return ValidationResult.failure(getClass(), errors);
    }

    public static boolean isItemValid(ReceiptItem item) {
        MyBigDecimal pricePerUnit = item.getPricePerUnit();
        MyBigDecimal count = item.getCount();
        MyBigDecimal discount = item.getDiscount();
        MyBigDecimal finalCost = item.getFinalCost();
        if (pricePerUnit.isError() || count.isError() || discount.isError() || finalCost.isError()) {
            return false;
        }

        BigDecimal fullPrice = count.value().multiply(pricePerUnit.value());
        BigDecimal discountedPrice = fullPrice.add(discount.value()).setScale(2, RoundingMode.HALF_UP);
        return discountedPrice.compareTo(finalCost.value()) == 0;
    }
}
