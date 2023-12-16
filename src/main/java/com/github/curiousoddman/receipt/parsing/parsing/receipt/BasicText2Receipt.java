package com.github.curiousoddman.receipt.parsing.parsing.receipt;

import com.github.curiousoddman.receipt.parsing.model.MyLocalDateTime;
import com.github.curiousoddman.receipt.parsing.model.Receipt;
import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.model.MyBigDecimal;
import com.github.curiousoddman.receipt.parsing.stats.ParsingStatsCollector;
import com.github.curiousoddman.receipt.parsing.tess.MyTessResult;

import java.util.Collection;


public abstract class BasicText2Receipt<T extends Context> implements Text2Receipt {

    @Override
    public Receipt parse(String fileName, MyTessResult myTessResult, ParsingStatsCollector parsingStatsCollector) {
        T context = getContext(myTessResult, parsingStatsCollector);
        return Receipt
                .builder()
                .fileName(fileName)
                .shopBrand(getShopBrand(context))
                .shopName(getShopName(context))
                .cashRegisterNumber(getCashRegisterNumber(context))
                .totalSavings(getTotalSavings(context))
                .totalPayment(getTotalPayment(context))
                .usedShopBrandMoney(getUsedShopBrandMoney(context))
                //.totalVat(getTotalVat(context))
                .shopBrandMoneyAccumulated(getShopBrandMoneyAccumulated(context))
                .documentNumber(getDocumentNumber(context))
                .receiptDateTime(getReceiptDateTime(context))
                .items(getItems(context))
                .build();
    }

    protected abstract T getContext(MyTessResult text, ParsingStatsCollector parsingStatsCollector);

    protected abstract String getShopBrand(T context);

    protected abstract String getShopName(T context);

    protected abstract String getCashRegisterNumber(T context);

    protected abstract MyBigDecimal getTotalSavings(T context);

    protected abstract MyBigDecimal getTotalPayment(T context);

//    protected abstract MyBigDecimal getTotalVat(T context);

    protected abstract MyBigDecimal getShopBrandMoneyAccumulated(T context);

    protected abstract String getDocumentNumber(T context);

    protected abstract MyLocalDateTime getReceiptDateTime(T context);

    protected abstract Collection<? extends ReceiptItem> getItems(T context);

    protected abstract MyBigDecimal getUsedShopBrandMoney(T context);
}
