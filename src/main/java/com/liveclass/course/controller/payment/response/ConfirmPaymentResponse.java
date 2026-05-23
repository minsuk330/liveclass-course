package com.liveclass.course.controller.payment.response;

import com.liveclass.course.domain.payment.PaymentStatus;
import com.liveclass.course.service.ports.in.result.payment.ConfirmPaymentResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfirmPaymentResponse(
        Long paymentId,
        PaymentStatus status,
        BigDecimal amount,
        LocalDateTime approvedAt,
        boolean success,
        String failedReason
) {
    public static ConfirmPaymentResponse from(ConfirmPaymentResult result) {
        return new ConfirmPaymentResponse(
                result.paymentId(),
                result.status(),
                result.amount(),
                result.approvedAt(),
                result.success(),
                result.failedReason()
        );
    }
}
