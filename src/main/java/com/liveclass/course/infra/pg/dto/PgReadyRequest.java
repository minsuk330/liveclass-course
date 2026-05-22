package com.liveclass.course.infra.pg.dto;

import java.math.BigDecimal;

public record PgReadyRequest(
        String orderId,
        BigDecimal amount,
        String productName,
        String buyerName
) {
}
