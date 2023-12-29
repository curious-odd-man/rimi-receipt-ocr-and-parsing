package com.github.curiousoddman.receipt.parsing.parsing.receipt;

import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.NumberOcrResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ReceiptItemResult {
    private final ReceiptItem     receiptItem;
    private final NumberOcrResult finalCostOcrResult;
    private final NumberOcrResult discountOcrResult;
    private final NumberOcrResult countOcrResult;
    private final NumberOcrResult pricePerUnitOcrResult;
}
