package com.github.curiousoddman.receipt.parsing.utils;

import java.util.regex.Pattern;

public class Patterns {

    public static final Pattern JUR_ADDR = Pattern.compile("Jur. adrese:.*");

    public static final Pattern INTEGER = Pattern.compile("-?\\d+");

    public static final Pattern MONEY_AMOUNT = Pattern.compile("[-+]?\\d+[.,]\\d\\d");

    public static final Pattern WEIGHT = Pattern.compile("\\d+[.,]\\d\\d\\d");

    /*
    1,076 kg X 2,99 EUR/kg 3,22 A
    1 gab X 3,19 EUR 371912
    1 gab X 1,29 EUR +292
     */
    public static final Pattern COUNT_PRICE_AND_SUM_LINE = Pattern.compile("(\\d+([.,]\\d+)?) +(gab|kg)y?g? +X +(\\d+([.,]\\d+)?) +EUR(/kg)? +(.*)");

    /*
    Atī. -0,33 Gala cena 2,89
    Atl. -0,36 Gala cena 1,29
    Atl. -0, 30 Gala cena 0,99
     */
    public static final Pattern ITEM_DISCOUNT_LINE_PATTERN = Pattern.compile("...\\.\\s+-*(-.*)\\s+Gala\\s+cena\\s+(.*)");

    public static final Pattern PAYMENT_SUM      = Pattern.compile("Samaksai EUR +(.*)");
    public static final Pattern TOTAL_AMOUNT     = Pattern.compile("KOPA: +(\\d+[.,]\\d+) +EUR.*");
    public static final Pattern BANK_CARD_AMOUNT = Pattern.compile("Bankas karte +(\\d+[.,]\\d+).*");

    public static final Pattern SAVINGS_AMOUNT_SEARCH = Pattern.compile("Tavs\\s+ietaupījums\\s+(.*)");

    /*
    Nodoklis Ar PWN Bez PVN PWN summa
    Nodoklis Ar PVN Bez PVN PVN summa
     */
    //public static final Pattern LINE_BEFORE_VAT_AMOUNTS_LINE = Pattern.compile(".*Nodoklis\\s+Ar\\s+...\\s+Bez\\s+...\\s+...\\s+summa.*");


    /*
    LAIKS 2022-05-28 13:16:08
     */
    public static final Pattern RECEIPT_TIME_PATTERN = Pattern.compile("LAIKS\\s+(.*)");

    /**
     * 1,.23:A
     * 1.23
     * 4
     * 0. 50
     * -1,20
     */
    public static final Pattern NUMBER_PATTERN = Pattern.compile(".*?-*?(-?\\d+)([.,\\s]+(\\d+))?.*");

    public static final Pattern NON_DIGITS = Pattern.compile("\\D+");
}
