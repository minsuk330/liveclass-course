package com.liveclass.course.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ClassErrorCode implements ErrorCode {

    CLASS_NOT_FOUND("CLASS_NOT_FOUND", "강의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CLASS_NOT_OPEN("CLASS_NOT_OPEN", "모집 중인 강의만 신청할 수 있습니다.", HttpStatus.BAD_REQUEST),
    CLASS_CAPACITY_EXCEEDED("CLASS_CAPACITY_EXCEEDED", "수강 정원이 초과되었습니다.", HttpStatus.CONFLICT),
    INVALID_CLASS_STATUS_TRANSITION("CLASS_INVALID_STATUS_TRANSITION", "잘못된 강의 상태 변경입니다.", HttpStatus.BAD_REQUEST),
    NOT_CLASS_OWNER("CLASS_NOT_OWNER", "본인 강의가 아닙니다.", HttpStatus.FORBIDDEN),
    CLASS_HAS_ACTIVE_ENROLLMENTS("CLASS_HAS_ACTIVE_ENROLLMENTS", "활성 신청이 있는 강의는 삭제할 수 없습니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String getCode() {
        return code;
    }
}
