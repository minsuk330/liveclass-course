package com.liveclass.course.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND("PAYMENT_NOT_FOUND", "결제를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_PAYMENT_STATUS("PAYMENT_INVALID_STATUS", "잘못된 결제 상태입니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_CANCELLED("PAYMENT_ALREADY_CANCELLED", "이미 취소된 결제입니다.", HttpStatus.CONFLICT),
    DUPLICATE_ACTIVE_PAYMENT("PAYMENT_DUPLICATE_ACTIVE", "이미 진행 중인 결제가 존재합니다.", HttpStatus.CONFLICT),
    PAYMENT_AMOUNT_MISMATCH("PAYMENT_AMOUNT_MISMATCH", "결제 금액이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    FORBIDDEN_PAYMENT_ACCESS("FORBIDDEN_PAYMENT_ACCESS", "본인의 결제가 아닙니다.", HttpStatus.FORBIDDEN),
    PG_APPROVAL_FAILED("PG_APPROVAL_FAILED", "결제 승인에 실패했습니다.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;

    @Override
    public String getCode() {
        return code;
    }
}
