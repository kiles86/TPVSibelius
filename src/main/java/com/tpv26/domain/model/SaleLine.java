package com.tpv26.domain.model;

import java.math.BigDecimal;

public record SaleLine(
    Long id,
    Long saleId,
    Product product,
    BigDecimal quantity,
    BigDecimal lineTotalWithVat,
    IvaType ivaType
) {
}
