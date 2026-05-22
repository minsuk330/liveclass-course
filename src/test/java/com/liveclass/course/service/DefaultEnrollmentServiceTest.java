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
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.EnrollmentErrorCode;
import com.liveclass.course.global.error.UserErrorCode;
import com.liveclass.course.repository.LiveClassRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DefaultEnrollmentServiceTest extends IntegrationTestBase {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private LiveClassService liveClassService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

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

    private LiveClass createOpenClass(int capacity) {
        LiveClass cls = liveClassService.create(new CreateClassCommand(
                creator.getId(),
                "Spring Boot 동시성",
                null,
                new BigDecimal("49000"),
                capacity,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(30)
        ));
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                cls.getId(), creator.getId(), ClassStatus.OPEN
        ));
        return cls;
    }

    @Test
    void 수강_신청_성공_PENDING_생성() {
        LiveClass cls = createOpenClass(10);

        Enrollment saved = enrollmentService.create(
                new CreateEnrollmentCommand(classmate.getId(), cls.getId())
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(saved.getUser().getId()).isEqualTo(classmate.getId());
        assertThat(saved.getLiveClass().getId()).isEqualTo(cls.getId());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPaidAt()).isNull();
    }

    @Test
    void 사용자_없음_USER_NOT_FOUND() {
        LiveClass cls = createOpenClass(10);

        assertThatThrownBy(() -> enrollmentService.create(
                new CreateEnrollmentCommand(999_999L, cls.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 강의_없음_CLASS_NOT_FOUND() {
        assertThatThrownBy(() -> enrollmentService.create(
                new CreateEnrollmentCommand(classmate.getId(), 999_999L)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_NOT_FOUND);
    }

    @Test
    void 강의_OPEN_아니면_CLASS_NOT_OPEN_DRAFT() {
        LiveClass draft = liveClassService.create(new CreateClassCommand(
                creator.getId(), "DRAFT 강의", null, new BigDecimal("10000"), 10,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));

        assertThatThrownBy(() -> enrollmentService.create(
                new CreateEnrollmentCommand(classmate.getId(), draft.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_NOT_OPEN);
    }

    @Test
    void 강의_OPEN_아니면_CLASS_NOT_OPEN_CLOSED() {
        LiveClass cls = createOpenClass(10);
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                cls.getId(), creator.getId(), ClassStatus.CLOSED
        ));

        assertThatThrownBy(() -> enrollmentService.create(
                new CreateEnrollmentCommand(classmate.getId(), cls.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_NOT_OPEN);
    }

    @Test
    void 중복_신청_DUPLICATE_ENROLLMENT() {
        LiveClass cls = createOpenClass(10);
        enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls.getId()));

        assertThatThrownBy(() -> enrollmentService.create(
                new CreateEnrollmentCommand(classmate.getId(), cls.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(EnrollmentErrorCode.DUPLICATE_ENROLLMENT);
    }

    @Test
    void 정원_초과_CLASS_CAPACITY_EXCEEDED() {
        LiveClass cls = createOpenClass(1);
        enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls.getId()));

        User another = userRepository.save(
                User.builder().name("수강생2").role(UserRole.CLASSMATE).build()
        );

        assertThatThrownBy(() -> enrollmentService.create(
                new CreateEnrollmentCommand(another.getId(), cls.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_CAPACITY_EXCEEDED);
    }
}
