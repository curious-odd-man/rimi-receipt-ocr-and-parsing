package com.github.curiousoddman.receipt.parsing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Discount {
    private String       text;
    private MyBigDecimal amount;
}
