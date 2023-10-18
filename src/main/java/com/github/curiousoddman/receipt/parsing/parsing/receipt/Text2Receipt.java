package com.github.curiousoddman.receipt.parsing.parsing.receipt;

import com.github.curiousoddman.receipt.parsing.model.Receipt;

public interface Text2Receipt {

    Receipt parse(String fileName, String text);
}
