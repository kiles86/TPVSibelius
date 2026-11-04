package com.tpv26.domain.model;

import java.math.BigDecimal;

public record Product(
    Long id,
    String name,
    ProductGroup group,
    BigDecimal defaultPriceWithVat,
    IvaType ivaType,
    int displayOrder,
    boolean active
) {
    @Override
    public String toString() {
        return name;
    }
}
