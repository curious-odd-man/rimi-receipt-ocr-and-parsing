package com.github.curiousoddman.receipt.parsing.parsing.receipt;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;

public interface Text2Receipt {

    Receipt parse(String fileName, MyTessResult text);
}
