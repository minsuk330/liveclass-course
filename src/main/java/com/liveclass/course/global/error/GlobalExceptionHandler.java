package com.liveclass.course.global.error;

import com.liveclass.course.domain.common.DomainErrorCode;
import com.liveclass.course.domain.common.DomainException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        logHandledException(errorCode, e);

        ErrorResponse body = e.getMeta() != null
                ? ErrorResponse.of(errorCode, e.getMeta())
                : ErrorResponse.of(errorCode);
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        ErrorCode errorCode = resolveDomainErrorCode(e.getErrorCode());
        logHandledException(errorCode, e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        Map<String, String> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage())
                                .orElse("올바르지 않은 값입니다."),
                        (oldValue, newValue) -> oldValue
                ));

        return ResponseEntity
                .status(CommonErrorCode.VALIDATION_ERROR.getStatus())
                .body(ErrorResponse.validation(fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e
    ) {
        Map<String, String> fieldErrors = e.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (oldValue, newValue) -> oldValue
                ));

        return ResponseEntity
                .status(CommonErrorCode.VALIDATION_ERROR.getStatus())
                .body(ErrorResponse.validation(fieldErrors));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequestException(Exception e) {
        ErrorCode errorCode = CommonErrorCode.INVALID_REQUEST;
        log.debug("Invalid request", e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(Exception e) {
        ErrorCode errorCode = CommonErrorCode.NOT_FOUND;
        log.debug("Resource not found", e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowedException(
            HttpRequestMethodNotSupportedException e
    ) {
        ErrorCode errorCode = CommonErrorCode.METHOD_NOT_ALLOWED;
        log.debug("HTTP method not allowed", e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaTypeException(
            HttpMediaTypeNotSupportedException e
    ) {
        ErrorCode errorCode = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
        log.debug("Unsupported media type", e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode));
    }

    private ErrorCode resolveDomainErrorCode(DomainErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_CLASS_STATUS_TRANSITION -> ClassErrorCode.INVALID_CLASS_STATUS_TRANSITION;
            case INVALID_ENROLLMENT_STATUS -> EnrollmentErrorCode.INVALID_ENROLLMENT_STATUS;
            case ENROLLMENT_ALREADY_CANCELLED -> EnrollmentErrorCode.ENROLLMENT_ALREADY_CANCELLED;
            case ENROLLMENT_CANCELLATION_PERIOD_EXPIRED -> EnrollmentErrorCode.CANCELLATION_PERIOD_EXPIRED;
            case INVALID_PAYMENT_STATUS -> PaymentErrorCode.INVALID_PAYMENT_STATUS;
            case PAYMENT_ALREADY_CANCELLED -> PaymentErrorCode.PAYMENT_ALREADY_CANCELLED;
        };
    }

    private void logHandledException(ErrorCode errorCode, RuntimeException e) {
        if (errorCode.getStatus().is5xxServerError()) {
            log.error("Handled server exception: {}", errorCode.getCode(), e);
            return;
        }

        log.warn("Handled client exception: {}, message={}", errorCode.getCode(), e.getMessage());
    }
}
