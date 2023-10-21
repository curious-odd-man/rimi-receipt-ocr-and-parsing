package com.github.curiousoddman.receipt.parsing.parsing;

import java.util.regex.Pattern;

public class Patterns {
    /*
    1,076 kg X 2,99 EUR/kg 3,22 A
    1 gab X 3,19 EUR 371912
     */
    public static final Pattern COUNT_PRICE_AND_SUM_LINE = Pattern.compile("(\\d+([.,]\\d+)?) (\\w+) X (\\d+([.,]\\d+)?) (\\w+|\\w+\\/\\w+) (\\d+([.,]\\d+)?)( \\w)?");

    /*
    Atī. -0,33 Gala cena 2,89
    Atl. -0,36 Gala cena 1,29
     */
    public static final Pattern ITEM_DISCOUNT_LINE_PATTERN = Pattern.compile("...\\.\\s+-(\\d+[.,]\\d+)\\s+Gala\\s+cena\\s+(\\d+[.,]\\d+).*");

    public static final Pattern PAYMENT_SUM      = Pattern.compile("Samaksai EUR +(.*)");
    public static final Pattern TOTAL_AMOUNT     = Pattern.compile("KOPA: +(\\d+[.,]\\d+) +EUR.*");
    public static final Pattern BANK_CARD_AMOUNT = Pattern.compile("Bankas karte +(\\d+[.,]\\d+).*");

}
