package com.liveclass.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.domain.user.UserRole;
import com.liveclass.course.global.error.ClassErrorCode;
import com.liveclass.course.global.error.CommonErrorCode;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.UserErrorCode;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.LiveClassRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.service.ports.in.result.liveclass.ClassDetail;
import com.liveclass.course.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DefaultLiveClassServiceTest extends IntegrationTestBase {

    @Autowired
    private LiveClassService liveClassService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

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

    private CreateClassCommand commandFor(Long creatorId) {
        return new CreateClassCommand(
                creatorId,
                "Spring Boot 동시성",
                "비관적 락 실습",
                new BigDecimal("49000"),
                30,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(30)
        );
    }

    @Test
    void 강의_등록_성공() {
        CreateClassCommand cmd = commandFor(creator.getId());

        LiveClass saved = liveClassService.create(cmd);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ClassStatus.DRAFT);
        assertThat(saved.getTitle()).isEqualTo("Spring Boot 동시성");
        assertThat(saved.getCreator().getId()).isEqualTo(creator.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCapacity()).isEqualTo(30);
    }

    @Test
    void 존재하지_않는_creator_USER_NOT_FOUND() {
        CreateClassCommand cmd = commandFor(999_999L);

        assertThatThrownBy(() -> liveClassService.create(cmd))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void creator_역할이_아니면_USER_NOT_CREATOR() {
        CreateClassCommand cmd = commandFor(classmate.getId());

        assertThatThrownBy(() -> liveClassService.create(cmd))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_CREATOR);
    }

    @Test
    void endDate가_startDate보다_빠르면_INVALID_REQUEST() {
        CreateClassCommand cmd = new CreateClassCommand(
                creator.getId(),
                "잘못된 기간",
                null,
                new BigDecimal("10000"),
                10,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5)
        );

        assertThatThrownBy(() -> liveClassService.create(cmd))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }

    @Test
    void endDate가_startDate와_같으면_INVALID_REQUEST() {
        LocalDate sameDate = LocalDate.now().plusDays(5);
        CreateClassCommand cmd = new CreateClassCommand(
                creator.getId(),
                "잘못된 기간",
                null,
                new BigDecimal("10000"),
                10,
                sameDate,
                sameDate
        );

        assertThatThrownBy(() -> liveClassService.create(cmd))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void 강의_상세_조회_성공_currentEnrolled_0() {
        LiveClass saved = liveClassService.create(commandFor(creator.getId()));

        ClassDetail detail = liveClassService.get(saved.getId());

        assertThat(detail.liveClass().getId()).isEqualTo(saved.getId());
        assertThat(detail.liveClass().getTitle()).isEqualTo("Spring Boot 동시성");
        assertThat(detail.liveClass().getCreator().getId()).isEqualTo(creator.getId());
        assertThat(detail.currentEnrolled()).isZero();
    }

    @Test
    void 강의_상세_조회_PENDING_CONFIRMED만_카운트_CANCELLED_제외() {
        LiveClass saved = liveClassService.create(commandFor(creator.getId()));

        enrollmentRepository.save(Enrollment.builder()
                .liveClass(saved).user(classmate)
                .status(EnrollmentStatus.PENDING).build());
        enrollmentRepository.save(Enrollment.builder()
                .liveClass(saved).user(classmate)
                .status(EnrollmentStatus.CONFIRMED).build());
        enrollmentRepository.save(Enrollment.builder()
                .liveClass(saved).user(classmate)
                .status(EnrollmentStatus.CANCELLED).build());

        ClassDetail detail = liveClassService.get(saved.getId());

        assertThat(detail.currentEnrolled()).isEqualTo(2L);
    }

    @Test
    void 존재하지_않는_강의_조회_CLASS_NOT_FOUND() {
        assertThatThrownBy(() -> liveClassService.get(999_999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_NOT_FOUND);
    }
}
