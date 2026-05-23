package com.liveclass.course.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WaitlistErrorCode implements ErrorCode {

    WAITLIST_NOT_FOUND("WAITLIST_NOT_FOUND", "대기열 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    WAITLIST_DUPLICATE("WAITLIST_DUPLICATE", "이미 대기열에 등록되어 있습니다.", HttpStatus.CONFLICT),
    CLASS_NOT_FULL("CLASS_NOT_FULL", "정원이 차지 않은 강의는 대기열 등록 불가합니다.", HttpStatus.BAD_REQUEST),
    ENROLLMENT_ALREADY_ACTIVE("ENROLLMENT_ALREADY_ACTIVE", "이미 활성 수강 신청이 있습니다.", HttpStatus.CONFLICT),
    FORBIDDEN_WAITLIST_ACCESS("FORBIDDEN_WAITLIST_ACCESS", "본인의 대기열 항목이 아닙니다.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String getCode() {
        return code;
    }
}
