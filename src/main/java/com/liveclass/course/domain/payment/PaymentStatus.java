package com.liveclass.course.domain.payment;

public enum PaymentStatus {
    READY,
    IN_PROGRESS,
    PAID,
    FAILED,
    CANCELLED;

    public boolean isActive() {
        return this == READY || this == IN_PROGRESS || this == PAID;
    }
}
