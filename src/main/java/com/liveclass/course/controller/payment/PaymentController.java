package com.liveclass.course.controller.payment;

import com.liveclass.course.controller.payment.request.PaymentCallbackRequest;
import com.liveclass.course.controller.payment.response.ConfirmPaymentResponse;
import com.liveclass.course.controller.payment.response.InitiatePaymentResponse;
import com.liveclass.course.service.ports.in.PaymentService;
import com.liveclass.course.service.ports.in.command.payment.ConfirmPaymentCommand;
import com.liveclass.course.service.ports.in.command.payment.InitiatePaymentCommand;
import com.liveclass.course.service.ports.in.result.payment.ConfirmPaymentResult;
import com.liveclass.course.service.ports.in.result.payment.InitiatePaymentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "[회원]결제", description = "결제 처리 API (NICE PG mock)")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/enrollments/{enrollmentId}/payments")
    @Operation(
            summary = "결제 요청 (Ready)",
            description = "Enrollment PENDING 상태에서 결제를 시작합니다. Mock PG에 결제창 요청 후 paymentUrl을 반환합니다."
    )
    public ResponseEntity<InitiatePaymentResponse> initiate(
            @PathVariable Long enrollmentId,
            @RequestParam @NotNull Long userId
    ) {
        InitiatePaymentResult result = paymentService.initiate(
                new InitiatePaymentCommand(enrollmentId, userId)
        );
        return ResponseEntity.ok(InitiatePaymentResponse.from(result));
    }

    @PostMapping("/payments/callback")
    @Operation(
            summary = "PG Callback (Webhook 모사)",
            description = "PG에서 서버로 결제 결과를 통보합니다. SUCCESS면 IN_PROGRESS, FAIL이면 FAILED 전환."
    )
    public ResponseEntity<Void> callback(
            @Valid @RequestBody PaymentCallbackRequest request
    ) {
        paymentService.handleCallback(request.toCommand());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/payments/{paymentId}/confirm")
    @Operation(
            summary = "결제 승인 확정 (Confirm)",
            description = "서버가 Mock PG에 승인 요청을 보내고, 성공 시 Payment PAID + Enrollment CONFIRMED 전환."
    )
    public ResponseEntity<ConfirmPaymentResponse> confirm(
            @PathVariable Long paymentId,
            @RequestParam @NotNull Long userId
    ) {
        ConfirmPaymentResult result = paymentService.confirm(
                new ConfirmPaymentCommand(paymentId, userId)
        );
        return ResponseEntity.ok(ConfirmPaymentResponse.from(result));
    }
}
