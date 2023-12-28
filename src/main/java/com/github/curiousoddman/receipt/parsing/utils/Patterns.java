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
    J1 gab X 5,49 EUR 5,49 A
    _1,210 kg X 1,59 EUR/kg 1,92 A
    0,654 kg X 1,49 EUR/ kg 0,97 F
    1 gab X 1,45 EU 1,45 A Atl. -0, 46 G
    1 iep X 39,99 EUR 39,99 A
    ll gab X 1,49 EUR 1,49 A
    1 gab X 2,87 EŪR 2,87 A
     */
    public static final Pattern COUNT_PRICE_AND_SUM_LINE = Pattern.compile(".?(.*) +.* +X +(\\d+([.,] ?\\d+)?)\\W+.{2,3}(/ ?kg)? +(.*)");

    /*
    Atī. -0,33 Gala cena 2,89
    Atl. -0,36 Gala cena 1,29
    Atl. -0, 30 Gala cena 0,99
    Atli -1,32
    _Atli -1,32
    „Atl.
    Atl. -0, 20 ala cena 0,79
     */
    public static final Pattern ITEM_DISCOUNT_LINE_PATTERN = Pattern.compile(".?....\\s+-*(-.*)\\s+G?ala\\s+cena\\s+(.*)");

    public static final Pattern PAYMENT_SUM              = Pattern.compile("Samaksai EUR +(.*)");
    public static final Pattern TOTAL_CARD_AMOUNT        = Pattern.compile("KOPA: +(\\d+[.,]\\d+) +EUR.*");
    public static final Pattern BANK_CARD_PAYMENT_AMOUNT = Pattern.compile("Bankas karte +(\\d+[.,]\\d+).*");
    public static final Pattern SAVINGS_AMOUNT_SEARCH    = Pattern.compile("Tavs\\s+ietaupījums\\s+(.*)");

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

    public static final Pattern SHOP_BRAND_MONEY_SPENT = Pattern.compile("Izmantot.\\s+Mans\\s+Rimi\\s+nauda.*");
    public static final Pattern DEPOZIT_COUNPON_LINE   = Pattern.compile("Depoz.ta\\s+kupons\\s+.*");
}
