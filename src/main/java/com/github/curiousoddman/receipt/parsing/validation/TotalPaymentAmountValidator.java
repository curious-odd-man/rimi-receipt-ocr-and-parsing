package com.github.curiousoddman.receipt.parsing.validation;

import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class TotalPaymentAmountValidator implements ReceiptValidator {
    @Override
    public ValidationResult validate(Receipt receipt) {
        MyBigDecimal receiptTotalPayment = receipt.getTotalAmount();
        if (receiptTotalPayment.isError()) {
            return ValidationResult.failure(getClass(), receiptTotalPayment.errorText());
        }

        BigDecimal paymentMethodSum = BigDecimal.ZERO;

        for (Map.Entry<String, List<MyBigDecimal>> paymentMethod : receipt.getPaymentMethods().entrySet()) {
            List<MyBigDecimal> amounts = paymentMethod.getValue();
            for (MyBigDecimal amount : amounts) {
                if (amount.isError()) {
                    return ValidationResult.failure(getClass(), amount.errorText());
                }
                paymentMethodSum = paymentMethodSum.add(amount.value());
            }
        }

        BigDecimal receiptTotalAmount = receiptTotalPayment.value();
        if (receiptTotalAmount.compareTo(paymentMethodSum) == 0) {
            return ValidationResult.success(getClass());
        }

        return ValidationResult.failure(
                getClass(),
                String.format("Total amount %s not equal to sum of payments %s", receiptTotalAmount, paymentMethodSum)
        );
    }
}
