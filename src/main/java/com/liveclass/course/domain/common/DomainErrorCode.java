package com.liveclass.course.domain.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DomainErrorCode {

    INVALID_CLASS_STATUS_TRANSITION("잘못된 강의 상태 변경입니다."),
    INVALID_ENROLLMENT_STATUS("잘못된 수강 신청 상태입니다."),
    ENROLLMENT_ALREADY_CANCELLED("이미 취소된 수강 신청입니다."),
    INVALID_PAYMENT_STATUS("잘못된 결제 상태입니다."),
    PAYMENT_ALREADY_CANCELLED("이미 취소된 결제입니다.");

    private final String message;
}
