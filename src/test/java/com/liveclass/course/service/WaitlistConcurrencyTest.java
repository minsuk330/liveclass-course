package com.liveclass.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.domain.user.UserRole;
import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.repository.WaitlistEntryRepository;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.service.ports.in.WaitlistService;
import com.liveclass.course.service.ports.in.command.enrollment.CancelEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.service.ports.in.command.waitlist.RegisterWaitlistCommand;
import com.liveclass.course.support.IntegrationConcurrencyTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WaitlistConcurrencyTest extends IntegrationConcurrencyTestBase {

    @Autowired
    private WaitlistService waitlistService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private LiveClassService liveClassService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Test
    void 동시_대기열_등록_5명_position_1_5_충돌없이_할당() throws Exception {
        // given
        User creator = userRepository.save(User.builder().name("강사").role(UserRole.CREATOR).build());
        LiveClass cls = liveClassService.create(new CreateClassCommand(
                creator.getId(), "강의", null, new BigDecimal("10000"), 1,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                cls.getId(), creator.getId(), ClassStatus.OPEN
        ));
        // 정원 차게 한 명 신청
        User taker = userRepository.save(User.builder().name("점유자").role(UserRole.CLASSMATE).build());
        enrollmentService.create(new CreateEnrollmentCommand(taker.getId(), cls.getId()));

        int n = 5;
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            User u = userRepository.save(User.builder().name("대기" + i).role(UserRole.CLASSMATE).build());
            userIds.add(u.getId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(n);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        for (Long uid : userIds) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    waitlistService.register(new RegisterWaitlistCommand(cls.getId(), uid));
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean done = endGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(done).isTrue();
        assertThat(success.get()).isEqualTo(n);
        assertThat(failed.get()).isZero();

        long count = waitlistEntryRepository.countByLiveClass_Id(cls.getId());
        assertThat(count).isEqualTo(n);
        // position 1..n 모두 존재
        List<WaitlistEntry> all = waitlistEntryRepository.findAll();
        assertThat(all).extracting(WaitlistEntry::getPosition)
                .containsExactlyInAnyOrder(1, 2, 3, 4, 5);
    }

    @Test
    void 정원_1_CONFIRMED_2명_동시_cancel_정확히_1명만_promote() throws Exception {
        User creator = userRepository.save(User.builder().name("강사").role(UserRole.CREATOR).build());
        LiveClass cls = liveClassService.create(new CreateClassCommand(
                creator.getId(), "강의", null, new BigDecimal("10000"), 2,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                cls.getId(), creator.getId(), ClassStatus.OPEN
        ));

        // 정원 2 → 2명 CONFIRMED
        User a = userRepository.save(User.builder().name("A").role(UserRole.CLASSMATE).build());
        User b = userRepository.save(User.builder().name("B").role(UserRole.CLASSMATE).build());
        Enrollment ea = enrollmentService.create(new CreateEnrollmentCommand(a.getId(), cls.getId()));
        Enrollment eb = enrollmentService.create(new CreateEnrollmentCommand(b.getId(), cls.getId()));
        ea.confirmPayment();
        eb.confirmPayment();
        enrollmentRepository.save(ea);
        enrollmentRepository.save(eb);

        // 대기열 3명 등록
        User w1 = userRepository.save(User.builder().name("W1").role(UserRole.CLASSMATE).build());
        User w2 = userRepository.save(User.builder().name("W2").role(UserRole.CLASSMATE).build());
        User w3 = userRepository.save(User.builder().name("W3").role(UserRole.CLASSMATE).build());
        waitlistService.register(new RegisterWaitlistCommand(cls.getId(), w1.getId()));
        waitlistService.register(new RegisterWaitlistCommand(cls.getId(), w2.getId()));
        waitlistService.register(new RegisterWaitlistCommand(cls.getId(), w3.getId()));

        // when: A,B 동시 cancel → 각각 promote 1번씩
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(2);

        executor.submit(() -> {
            try {
                startGate.await();
                enrollmentService.cancel(new CancelEnrollmentCommand(ea.getId(), a.getId()));
            } catch (Exception ignored) {
            } finally {
                endGate.countDown();
            }
        });
        executor.submit(() -> {
            try {
                startGate.await();
                enrollmentService.cancel(new CancelEnrollmentCommand(eb.getId(), b.getId()));
            } catch (Exception ignored) {
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean done = endGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(done).isTrue();

        // then: 대기열 1번(w1), 2번(w2) 정확히 PENDING 신규 enrollment, 3번(w3) 그대로
        long activeEnrollments = enrollmentRepository.countByLiveClass_IdAndStatusIn(
                cls.getId(),
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );
        assertThat(activeEnrollments).isEqualTo(2L);

        long remainingWaitlist = waitlistEntryRepository.countByLiveClass_Id(cls.getId());
        assertThat(remainingWaitlist).isEqualTo(1L);
    }
}
