package com.liveclass.course.controller.payment.request;

import com.liveclass.course.service.ports.in.command.payment.PaymentCallbackCommand;
import com.liveclass.course.service.ports.in.command.payment.PaymentCallbackCommand.PaymentCallbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentCallbackRequest(
        @NotBlank
        @Schema(description = "PG 거래 ID", example = "nicepay_abc123")
        String tid,

        @NotBlank
        @Schema(description = "PG 인증 토큰", example = "auth_xyz")
        String authToken,

        @NotNull
        @Schema(description = "PG 결제 결과", example = "SUCCESS")
        PaymentCallbackStatus status,

        @Schema(description = "실패 사유 (status=FAIL일 때)", example = "카드 한도 초과")
        String failReason
) {
    public PaymentCallbackCommand toCommand() {
        return new PaymentCallbackCommand(tid, authToken, status, failReason);
    }
}
