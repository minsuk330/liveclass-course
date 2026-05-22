package com.liveclass.course.infra.pg.dto;

import java.math.BigDecimal;

public record PgApproveRequest(
        String tid,
        String authToken,
        BigDecimal amount
) {
}
