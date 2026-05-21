package com.liveclass.course.domain.common;

import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

    private final DomainErrorCode errorCode;
    private final String detail;

    public DomainException(DomainErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public DomainException(DomainErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = detail;
    }
}
