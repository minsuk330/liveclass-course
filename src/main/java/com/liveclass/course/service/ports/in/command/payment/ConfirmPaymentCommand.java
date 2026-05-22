package com.liveclass.course.service.ports.in.command.payment;

public record ConfirmPaymentCommand(
        Long paymentId,
        Long userId
) {
}
