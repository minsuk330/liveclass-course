package com.liveclass.course.infra.pg.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PgApproveResponse(
        boolean success,
        String tid,
        BigDecimal approvedAmount,
        LocalDateTime approvedAt,
        String resultCode,
        String resultMessage
) {
    public static PgApproveResponse ok(String tid, BigDecimal amount) {
        return new PgApproveResponse(true, tid, amount, LocalDateTime.now(), "0000", "정상 승인");
    }

    public static PgApproveResponse fail(String tid, String code, String message) {
        return new PgApproveResponse(false, tid, null, null, code, message);
    }
}
