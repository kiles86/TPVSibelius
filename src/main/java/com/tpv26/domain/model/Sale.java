package com.tpv26.domain.model;

import java.time.LocalDateTime;

public record Sale(
    Long id,
    LocalDateTime createdAt,
    SaleStatus status,
    PaymentMethod paymentMethod,
    Long cashSessionId,
    Long tableId,
    boolean barSale
) {
}
