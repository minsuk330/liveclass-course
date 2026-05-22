package com.liveclass.course.service.ports.in.command.payment;

public record InitiatePaymentCommand(
        Long enrollmentId,
        Long userId
) {
}
