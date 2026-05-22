package com.liveclass.course.service.ports.in.command.payment;

public record PaymentCallbackCommand(
        String tid,
        String authToken,
        PaymentCallbackStatus status,
        String failReason
) {
    public enum PaymentCallbackStatus {
        SUCCESS,
        FAIL
    }
}
