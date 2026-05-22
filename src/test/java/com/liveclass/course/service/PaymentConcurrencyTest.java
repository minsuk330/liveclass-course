package com.liveclass.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.payment.Payment;
import com.liveclass.course.domain.payment.PaymentStatus;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.domain.user.UserRole;
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
import com.liveclass.course.support.IntegrationConcurrencyTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentConcurrencyTest extends IntegrationConcurrencyTestBase {

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

    @Test
    void 같은_payment_동시_confirm_5번_정확히_1번만_PAID() throws Exception {
        User creator = userRepository.save(
                User.builder().name("강사").role(UserRole.CREATOR).build()
        );
        User classmate = userRepository.save(
                User.builder().name("수강생").role(UserRole.CLASSMATE).build()
        );
        LiveClass cls = liveClassService.create(new CreateClassCommand(
                creator.getId(), "강의", null, new BigDecimal("49000"), 10,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                cls.getId(), creator.getId(), ClassStatus.OPEN
        ));
        Enrollment enrollment = enrollmentService.create(
                new CreateEnrollmentCommand(classmate.getId(), cls.getId())
        );
        InitiatePaymentResult initResult = paymentService.initiate(
                new InitiatePaymentCommand(enrollment.getId(), classmate.getId())
        );
        paymentService.handleCallback(new PaymentCallbackCommand(
                initResult.tid(), "auth_token", PaymentCallbackStatus.SUCCESS, null
        ));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    paymentService.confirm(new ConfirmPaymentCommand(
                            initResult.paymentId(), classmate.getId()
                    ));
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = endGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(success.get()).isEqualTo(1);
        assertThat(failed.get()).isEqualTo(threadCount - 1);

        Payment payment = paymentRepository.findById(initResult.paymentId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);

        Enrollment confirmed = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    }
}
