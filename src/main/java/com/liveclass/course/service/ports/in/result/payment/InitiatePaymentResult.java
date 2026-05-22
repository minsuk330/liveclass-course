package com.liveclass.course.service.ports.in.result.payment;

import com.liveclass.course.domain.payment.Payment;
import java.math.BigDecimal;

public record InitiatePaymentResult(
        Long paymentId,
        String tid,
        String paymentUrl,
        BigDecimal amount
) {
    public static InitiatePaymentResult of(Payment payment, String paymentUrl) {
        return new InitiatePaymentResult(
                payment.getId(),
                payment.getTid(),
                paymentUrl,
                payment.getAmount()
        );
    }
}
