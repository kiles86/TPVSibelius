package com.tpv26.application.dto;

import java.math.BigDecimal;

public record SaleTotals(BigDecimal base, BigDecimal vat, BigDecimal total) {
}
