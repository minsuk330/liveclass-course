package com.liveclass.course.service;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.payment.Payment;
import com.liveclass.course.domain.payment.PaymentMethod;
import com.liveclass.course.domain.payment.PaymentStatus;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.EnrollmentErrorCode;
import com.liveclass.course.global.error.PaymentErrorCode;
import com.liveclass.course.infra.pg.PgClient;
import com.liveclass.course.infra.pg.dto.PgApproveRequest;
import com.liveclass.course.infra.pg.dto.PgApproveResponse;
import com.liveclass.course.infra.pg.dto.PgReadyRequest;
import com.liveclass.course.infra.pg.dto.PgReadyResponse;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.PaymentRepository;
import com.liveclass.course.service.ports.in.PaymentService;
import com.liveclass.course.service.ports.in.command.payment.ConfirmPaymentCommand;
import com.liveclass.course.service.ports.in.command.payment.InitiatePaymentCommand;
import com.liveclass.course.service.ports.in.command.payment.PaymentCallbackCommand;
import com.liveclass.course.service.ports.in.result.payment.ConfirmPaymentResult;
import com.liveclass.course.service.ports.in.result.payment.InitiatePaymentResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class DefaultPaymentService implements PaymentService {

    private static final List<PaymentStatus> ACTIVE_STATUSES =
            List.of(PaymentStatus.READY, PaymentStatus.IN_PROGRESS, PaymentStatus.PAID);

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PgClient pgClient;

    @Override
    @Transactional
    public InitiatePaymentResult initiate(InitiatePaymentCommand command) {

        Enrollment enrollment = enrollmentRepository.findById(command.enrollmentId())
                .orElseThrow(() -> new CustomException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getUser().getId().equals(command.userId())) {
            throw new CustomException(EnrollmentErrorCode.FORBIDDEN_ENROLLMENT_ACCESS);
        }

        if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
            throw new CustomException(EnrollmentErrorCode.INVALID_ENROLLMENT_STATUS);
        }

        if (paymentRepository.existsByEnrollment_IdAndStatusIn(
                command.enrollmentId(), ACTIVE_STATUSES)) {
            throw new CustomException(PaymentErrorCode.DUPLICATE_ACTIVE_PAYMENT);
        }

        PgReadyResponse pgReady = pgClient.ready(new PgReadyRequest(
                String.valueOf(enrollment.getId()),
                enrollment.getLiveClass().getPrice(),
                enrollment.getLiveClass().getTitle(),
                enrollment.getUser().getName()
        ));

        Payment payment = Payment.builder()
                .enrollment(enrollment)
                .amount(enrollment.getLiveClass().getPrice())
                .method(PaymentMethod.CARD)
                .tid(pgReady.tid())
                .build();
        Payment saved = paymentRepository.save(payment);

        return InitiatePaymentResult.of(saved, pgReady.paymentUrl());
    }

    @Override
    @Transactional
    public void handleCallback(PaymentCallbackCommand command) {
        Payment payment = paymentRepository.findByTid(command.tid())
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        switch (command.status()) {
            case SUCCESS -> payment.markInProgress(command.authToken());
            case FAIL -> payment.fail(command.failReason());
        }
    }

    @Override
    @Transactional
    public ConfirmPaymentResult confirm(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findByIdForUpdate(command.paymentId())
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.getEnrollment().getUser().getId().equals(command.userId())) {
            throw new CustomException(PaymentErrorCode.FORBIDDEN_PAYMENT_ACCESS);
        }

        PgApproveResponse pgApprove = pgClient.approve(new PgApproveRequest(
                payment.getTid(),
                payment.getAuthToken(),
                payment.getAmount()
        ));

        if (!pgApprove.success()) {
            payment.fail(pgApprove.resultMessage());
            return ConfirmPaymentResult.failure(payment, pgApprove.resultMessage());
        }

        payment.approve();
        payment.getEnrollment().confirmPayment();
        return ConfirmPaymentResult.success(payment);
    }
}
