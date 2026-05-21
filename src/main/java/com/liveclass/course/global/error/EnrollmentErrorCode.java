package com.liveclass.course.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EnrollmentErrorCode implements ErrorCode {

    ENROLLMENT_NOT_FOUND("ENROLLMENT_NOT_FOUND", "수강 신청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_ENROLLMENT("ENROLLMENT_DUPLICATE", "이미 신청한 강의입니다.", HttpStatus.CONFLICT),
    INVALID_ENROLLMENT_STATUS("ENROLLMENT_INVALID_STATUS", "잘못된 수강 신청 상태입니다.", HttpStatus.BAD_REQUEST),
    ENROLLMENT_ALREADY_CANCELLED("ENROLLMENT_ALREADY_CANCELLED", "이미 취소된 수강 신청입니다.", HttpStatus.CONFLICT),
    CANCELLATION_PERIOD_EXPIRED("ENROLLMENT_CANCELLATION_PERIOD_EXPIRED", "취소 가능 기간이 지났습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String getCode() {
        return code;
    }
}
