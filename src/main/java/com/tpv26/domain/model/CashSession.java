package com.tpv26.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CashSession(
    Long id,
    Long userId,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    BigDecimal openingAmount,
    boolean open
) {
}
