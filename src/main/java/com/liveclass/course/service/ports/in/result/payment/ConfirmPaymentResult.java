package com.liveclass.course.service.ports.in.result.payment;

import com.liveclass.course.domain.payment.Payment;
import com.liveclass.course.domain.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfirmPaymentResult(
        Long paymentId,
        PaymentStatus status,
        BigDecimal amount,
        LocalDateTime approvedAt,
        boolean success,
        String failedReason
) {
    public static ConfirmPaymentResult success(Payment payment) {
        return new ConfirmPaymentResult(
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getApprovedAt(),
                true,
                null
        );
    }

    public static ConfirmPaymentResult failure(Payment payment, String reason) {
        return new ConfirmPaymentResult(
                payment.getId(),
                payment.getStatus(),
                payment.getAmount(),
                null,
                false,
                reason
        );
    }
}
