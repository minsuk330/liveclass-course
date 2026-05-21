package com.liveclass.course.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    public static ErrorResponse validation(Map<String, String> fieldErrors) {
        return new ErrorResponse(
                CommonErrorCode.VALIDATION_ERROR.getCode(),
                CommonErrorCode.VALIDATION_ERROR.getMessage(),
                fieldErrors
        );
    }
}
