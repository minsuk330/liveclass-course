package com.liveclass.course.controller.payment.response;

import com.liveclass.course.service.ports.in.result.payment.InitiatePaymentResult;
import java.math.BigDecimal;

public record InitiatePaymentResponse(
        Long paymentId,
        String tid,
        String paymentUrl,
        BigDecimal amount
) {
    public static InitiatePaymentResponse from(InitiatePaymentResult result) {
        return new InitiatePaymentResponse(
                result.paymentId(),
                result.tid(),
                result.paymentUrl(),
                result.amount()
        );
    }
}
