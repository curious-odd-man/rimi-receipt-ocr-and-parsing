package com.github.curiousoddman.receipt.parsing.parsing.receipt.rimi;

import com.github.curiousoddman.receipt.parsing.model.ReceiptItem;
import com.github.curiousoddman.receipt.parsing.parsing.receipt.BasicText2Receipt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class RimiText2Receipt extends BasicText2Receipt<RimiContext> {
    @Override
    protected RimiContext getContext(String text) {
        return new RimiContext(text);
    }

    @Override
    protected String getShopBrand(RimiContext context) {
        return "Rimi";
    }

    @Override
    protected String getShopName(RimiContext context) {
        return context.getLineContaining("Rīga", 1);
    }

    @Override
    protected String getCashRegisterNumber(RimiContext context) {
        return context.getLineContaining("Kase Nr", 0);
    }

    @Override
    protected BigDecimal getTotalSavings(RimiContext context) {
        String text = "Nopelnītā Mans Rimi nauda";
        String line = context.getLineContaining(text, 0);
        return new BigDecimal(line.split(text)[0].trim());
    }

    @Override
    protected BigDecimal getTotalPayment(RimiContext context) {
        String text = "Samaksai EUR";
        String line = context.getLineContaining(text, 0);
        return new BigDecimal(line.split(text)[0].trim());
    }

    @Override
    protected BigDecimal getTotalVat(RimiContext context) {
        String text = "Nodoklis Ar PVN Bez PVN PVN summa";
        String line = context.getLinesAfterContaining(text).get(0);
        return new BigDecimal(line.split(" ")[5].trim());
    }

    @Override
    protected BigDecimal getShopBrandMoneyAccumulated(RimiContext context) {
        String text = "Mans Rimi naudas uzkrājums";
        String line = context.getLineContaining(text, 0);
        return new BigDecimal(line.split(text)[0].trim());
    }

    @Override
    protected String getDocumentNumber(RimiContext context) {
        String text = "Dok. Nr.:";
        String line = context.getLineContaining(text, 0);
        return line.split(text)[0].trim();
    }

    @Override
    protected LocalDateTime getReceiptDateTime(RimiContext context) {
        return LocalDateTime.parse(context.getLine(-3));
    }

    @Override
    protected Collection<? extends ReceiptItem> getItems(RimiContext context) {
        List<ReceiptItem> items = new ArrayList<>();
        List<String> linesBetween = context.getLinesBetween("KLIENTS:", "Maksājumu karte");
        return items;
    }
}
