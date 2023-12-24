package com.github.curiousoddman.receipt.parsing.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Translations {

    public String translateDiscountName(String name) {
        if (name.equals("Citas akcijas")) {
            return Constants.DISCOUNTS_SUM;
        } else if (Patterns.SHOP_BRAND_MONEY_SPENT.matcher(name).matches()) {
            return Constants.USED_SHOP_BRAND_MONEY;
        } else {
            return name;
        }
    }
}
