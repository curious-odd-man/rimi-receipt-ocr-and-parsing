package com.github.curiousoddman.receipt.parsing.parsing.receipt;

import com.github.curiousoddman.receipt.parsing.model.Receipt;
import org.springframework.stereotype.Component;

@Component
public class BasicText2Receipt implements Text2Receipt {

    @Override
    public Receipt parse(String text) {
        return null;
    }
}
