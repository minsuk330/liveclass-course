package com.liveclass.course.global.error;

import java.util.Map;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String detail;
    private final Map<String, Object> meta;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
        this.meta = null;
    }

    public CustomException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
        this.meta = null;
    }

    public CustomException(ErrorCode errorCode, Map<String, Object> meta) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
        this.meta = meta;
    }
}
