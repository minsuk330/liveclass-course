package com.liveclass.course.infra.pg.dto;

public record PgReadyResponse(
        String tid,
        String paymentUrl
) {
}
