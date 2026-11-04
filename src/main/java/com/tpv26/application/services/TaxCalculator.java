package com.tpv26.application.services;

import com.tpv26.application.dto.SaleTotals;
import com.tpv26.domain.model.SaleLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class TaxCalculator {
    public SaleTotals calculate(List<SaleLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal base = BigDecimal.ZERO;

        for (SaleLine line : lines) {
            BigDecimal lineTotal = line.lineTotalWithVat();
            BigDecimal divisor = BigDecimal.ONE.add(line.ivaType().rate());
            BigDecimal lineBase = lineTotal.divide(divisor, 2, RoundingMode.HALF_UP);
            total = total.add(lineTotal);
            base = base.add(lineBase);
        }

        total = total.setScale(2, RoundingMode.HALF_UP);
        base = base.setScale(2, RoundingMode.HALF_UP);
        BigDecimal vat = total.subtract(base).setScale(2, RoundingMode.HALF_UP);
        return new SaleTotals(base, vat, total);
    }
}
