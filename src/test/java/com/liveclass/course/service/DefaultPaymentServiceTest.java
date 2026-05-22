package com.liveclass.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveclass.course.domain.common.DomainErrorCode;
import com.liveclass.course.domain.common.DomainException;
import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.payment.Payment;
import com.liveclass.course.domain.payment.PaymentStatus;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.domain.user.UserRole;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.EnrollmentErrorCode;
import com.liveclass.course.global.error.PaymentErrorCode;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.PaymentRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.service.ports.in.PaymentService;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.service.ports.in.command.payment.ConfirmPaymentCommand;
import com.liveclass.course.service.ports.in.command.payment.InitiatePaymentCommand;
import com.liveclass.course.service.ports.in.command.payment.PaymentCallbackCommand;
import com.liveclass.course.service.ports.in.command.payment.PaymentCallbackCommand.PaymentCallbackStatus;
import com.liveclass.course.service.ports.in.result.payment.InitiatePaymentResult;
import com.liveclass.course.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DefaultPaymentServiceTest extends IntegrationTestBase {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private LiveClassService liveClassService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private User creator;
    private User classmate;

    @BeforeEach
    void seed() {
        creator = userRepository.save(
                User.builder().name("강사").role(UserRole.CREATOR).build()
        );
        classmate = userRepository.save(
                User.builder().name("수강생").role(UserRole.CLASSMATE).build()
        );
    }

    private Enrollment createPendingEnrollment() {
        LiveClass cls = liveClassService.create(new CreateClassCommand(
                creator.getId(), "강의", null, new BigDecimal("49000"), 10,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                cls.getId(), creator.getId(), ClassStatus.OPEN
        ));
        return enrollmentService.create(
                new CreateEnrollmentCommand(classmate.getId(), cls.getId())
        );
    }

    private Payment createReadyPayment(Enrollment enrollment) {
        InitiatePaymentResult result = paymentService.initiate(
                new InitiatePaymentCommand(enrollment.getId(), enrollment.getUser().getId())
        );
        return paymentRepository.findById(result.paymentId()).orElseThrow();
    }

    // ---------- initiate ----------

    @Test
    void 결제_요청_성공_READY_상태와_paymentUrl_반환() {
        Enrollment e = createPendingEnrollment();

        InitiatePaymentResult result = paymentService.initiate(
                new InitiatePaymentCommand(e.getId(), classmate.getId())
        );

        assertThat(result.paymentId()).isNotNull();
        assertThat(result.tid()).startsWith("nicepay_");
        assertThat(result.paymentUrl()).startsWith("https://mock-nicepay.test/pay/");
        assertThat(result.amount()).isEqualByComparingTo("49000");

        Payment saved = paymentRepository.findById(result.paymentId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(saved.getAuthToken()).isNull();
    }

    @Test
    void 결제_요청_Enrollment_없음_ENROLLMENT_NOT_FOUND() {
        assertThatThrownBy(() -> paymentService.initiate(
                new InitiatePaymentCommand(999_999L, classmate.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND);
    }

    @Test
    void 결제_요청_본인_아니면_FORBIDDEN_ENROLLMENT_ACCESS() {
        Enrollment e = createPendingEnrollment();
        User other = userRepository.save(
                User.builder().name("타인").role(UserRole.CLASSMATE).build()
        );

        assertThatThrownBy(() -> paymentService.initiate(
                new InitiatePaymentCommand(e.getId(), other.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(EnrollmentErrorCode.FORBIDDEN_ENROLLMENT_ACCESS);
    }

    @Test
    void 결제_요청_중복_활성_PAYMENT_DUPLICATE_ACTIVE() {
        Enrollment e = createPendingEnrollment();
        paymentService.initiate(new InitiatePaymentCommand(e.getId(), classmate.getId()));

        assertThatThrownBy(() -> paymentService.initiate(
                new InitiatePaymentCommand(e.getId(), classmate.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.DUPLICATE_ACTIVE_PAYMENT);
    }

    // ---------- handleCallback ----------

    @Test
    void Callback_SUCCESS_READY를_IN_PROGRESS로_authToken_저장() {
        Enrollment e = createPendingEnrollment();
        Payment p = createReadyPayment(e);

        paymentService.handleCallback(new PaymentCallbackCommand(
                p.getTid(), "auth_token_xyz", PaymentCallbackStatus.SUCCESS, null
        ));

        Payment reloaded = paymentRepository.findById(p.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
        assertThat(reloaded.getAuthToken()).isEqualTo("auth_token_xyz");
    }

    @Test
    void Callback_tid_없음_PAYMENT_NOT_FOUND() {
        assertThatThrownBy(() -> paymentService.handleCallback(new PaymentCallbackCommand(
                "nicepay_unknown", "token", PaymentCallbackStatus.SUCCESS, null
        )))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    void Callback_FAIL_FAILED_전환_failReason_저장() {
        Enrollment e = createPendingEnrollment();
        Payment p = createReadyPayment(e);

        paymentService.handleCallback(new PaymentCallbackCommand(
                p.getTid(), null, PaymentCallbackStatus.FAIL, "카드 한도 초과"
        ));

        Payment reloaded = paymentRepository.findById(p.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(reloaded.getFailedReason()).isEqualTo("카드 한도 초과");
    }

    @Test
    void Callback_이미_IN_PROGRESS면_DomainException() {
        Enrollment e = createPendingEnrollment();
        Payment p = createReadyPayment(e);
        paymentService.handleCallback(new PaymentCallbackCommand(
                p.getTid(), "token", PaymentCallbackStatus.SUCCESS, null
        ));

        assertThatThrownBy(() -> paymentService.handleCallback(new PaymentCallbackCommand(
                p.getTid(), "token2", PaymentCallbackStatus.SUCCESS, null
        )))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.INVALID_PAYMENT_STATUS);
    }

    // ---------- confirm ----------

    @Test
    void Confirm_성공_PAID_Enrollment_CONFIRMED_전환() {
        Enrollment e = createPendingEnrollment();
        Payment p = createReadyPayment(e);
        paymentService.handleCallback(new PaymentCallbackCommand(
                p.getTid(), "auth_token", PaymentCallbackStatus.SUCCESS, null
        ));

        paymentService.confirm(new ConfirmPaymentCommand(p.getId(), classmate.getId()));

        Payment confirmedPayment = paymentRepository.findById(p.getId()).orElseThrow();
        assertThat(confirmedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(confirmedPayment.getApprovedAt()).isNotNull();

        Enrollment confirmedEnrollment = enrollmentRepository.findById(e.getId()).orElseThrow();
        assertThat(confirmedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(confirmedEnrollment.getPaidAt()).isNotNull();
    }

    @Test
    void Confirm_paymentId_없음_PAYMENT_NOT_FOUND() {
        assertThatThrownBy(() -> paymentService.confirm(
                new ConfirmPaymentCommand(999_999L, classmate.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    void Confirm_본인_아니면_FORBIDDEN_PAYMENT_ACCESS() {
        Enrollment e = createPendingEnrollment();
        Payment p = createReadyPayment(e);
        paymentService.handleCallback(new PaymentCallbackCommand(
                p.getTid(), "token", PaymentCallbackStatus.SUCCESS, null
        ));
        User other = userRepository.save(
                User.builder().name("타인").role(UserRole.CLASSMATE).build()
        );

        assertThatThrownBy(() -> paymentService.confirm(
                new ConfirmPaymentCommand(p.getId(), other.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.FORBIDDEN_PAYMENT_ACCESS);
    }

    @Test
    void Confirm_IN_PROGRESS_아니면_DomainException() {
        Enrollment e = createPendingEnrollment();
        Payment p = createReadyPayment(e);

        assertThatThrownBy(() -> paymentService.confirm(
                new ConfirmPaymentCommand(p.getId(), classmate.getId())
        ))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(DomainErrorCode.INVALID_PAYMENT_STATUS);
    }
}
