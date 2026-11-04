package com.tpv26.application.dto;

import com.tpv26.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.Map;

public record CashCloseSummary(BigDecimal openingAmount, Map<PaymentMethod, BigDecimal> paymentTotals) {
    public BigDecimal totalSales() {
        return paymentTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
