package com.tpv26.domain.model;

import java.math.BigDecimal;

public enum IvaType {
    REDUCIDO_10(new BigDecimal("0.10")),
    GENERAL_21(new BigDecimal("0.21")),
    SUPERREDUCIDO_4(new BigDecimal("0.04"));

    private final BigDecimal rate;

    IvaType(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal rate() {
        return rate;
    }
}
