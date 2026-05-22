package com.liveclass.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.domain.user.UserRole;
import com.liveclass.course.global.error.ClassErrorCode;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
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

class EnrollmentConcurrencyTest extends IntegrationConcurrencyTestBase {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private LiveClassService liveClassService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void 정원_1자리에_10명_동시_신청_시_정확히_1명만_성공() throws Exception {
        // given: 정원 1, OPEN 상태 강의 + 10명 사용자
        User creator = userRepository.save(
                User.builder().name("강사").role(UserRole.CREATOR).build()
        );
        LiveClass cls = liveClassService.create(new CreateClassCommand(
                creator.getId(),
                "테스트 강의",
                null,
                new BigDecimal("10000"),
                1,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(30)
        ));
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                cls.getId(), creator.getId(), ClassStatus.OPEN
        ));

        int threadCount = 10;
        List<Long> classmateIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            User u = userRepository.save(
                    User.builder().name("user" + i).role(UserRole.CLASSMATE).build()
            );
            classmateIds.add(u.getId());
        }

        // when: 동시 신청
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger capacityFail = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();

        for (Long userId : classmateIds) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    enrollmentService.create(new CreateEnrollmentCommand(userId, cls.getId()));
                    success.incrementAndGet();
                } catch (CustomException e) {
                    if (e.getErrorCode() == ClassErrorCode.CLASS_CAPACITY_EXCEEDED) {
                        capacityFail.incrementAndGet();
                    } else {
                        other.incrementAndGet();
                    }
                } catch (Exception e) {
                    other.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = endGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then: 정확히 1명 성공, 9명 정원 초과
        assertThat(finished).isTrue();
        assertThat(success.get()).isEqualTo(1);
        assertThat(capacityFail.get()).isEqualTo(threadCount - 1);
        assertThat(other.get()).isZero();

        long enrolledCount = enrollmentRepository.countByLiveClass_IdAndStatusIn(
                cls.getId(),
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );
        assertThat(enrolledCount).isEqualTo(1L);
    }
}
