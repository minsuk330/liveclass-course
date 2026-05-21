package com.liveclass.course.domain.liveclass;

public enum ClassStatus {
    DRAFT,
    OPEN,
    CLOSED;

    public boolean canTransitionTo(ClassStatus next) {
        return switch (this) {
            case DRAFT -> next == OPEN;
            case OPEN -> next == CLOSED;
            case CLOSED -> false;
        };
    }
}
