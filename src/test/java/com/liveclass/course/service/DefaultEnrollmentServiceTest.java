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
import com.liveclass.course.global.dto.EnrollmentSortType;
import com.liveclass.course.global.dto.SortDirection;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.enrollment.SearchMyEnrollmentsCommand;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.service.ports.in.result.enrollment.MyEnrollmentListItem;
import com.liveclass.course.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    // ---------- searchMyEnrollments ----------

    @Test
    void 내_수강_신청_목록_기본_조회_classTitle_price_포함() {
        LiveClass cls = createOpenClass(10);
        enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls.getId()));

        Pageable pageable = PageRequest.of(0, 10);
        Page<MyEnrollmentListItem> page = enrollmentService.searchMyEnrollments(
                new SearchMyEnrollmentsCommand(classmate.getId(), null, null, null),
                pageable
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        MyEnrollmentListItem item = page.getContent().get(0);
        assertThat(item.classId()).isEqualTo(cls.getId());
        assertThat(item.classTitle()).isEqualTo("Spring Boot 동시성");
        assertThat(item.status()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(item.price()).isEqualByComparingTo("49000");
        assertThat(item.createdAt()).isNotNull();
        assertThat(item.paidAt()).isNull();
        assertThat(item.cancelledAt()).isNull();
    }

    @Test
    void 내_수강_신청_빈_결과() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<MyEnrollmentListItem> page = enrollmentService.searchMyEnrollments(
                new SearchMyEnrollmentsCommand(classmate.getId(), null, null, null),
                pageable
        );

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void 사용자_없으면_USER_NOT_FOUND() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> enrollmentService.searchMyEnrollments(
                new SearchMyEnrollmentsCommand(999_999L, null, null, null),
                pageable
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    void status_필터_PENDING만() {
        LiveClass cls1 = createOpenClass(10);
        LiveClass cls2 = createOpenClass(10);
        Enrollment e1 = enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls1.getId()));
        Enrollment e2 = enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls2.getId()));
        e2.cancel();

        Pageable pageable = PageRequest.of(0, 10);
        Page<MyEnrollmentListItem> page = enrollmentService.searchMyEnrollments(
                new SearchMyEnrollmentsCommand(
                        classmate.getId(), EnrollmentStatus.PENDING, null, null
                ),
                pageable
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).enrollmentId()).isEqualTo(e1.getId());
    }

    @Test
    void status_필터_CANCELLED만() {
        LiveClass cls1 = createOpenClass(10);
        LiveClass cls2 = createOpenClass(10);
        enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls1.getId()));
        Enrollment e2 = enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls2.getId()));
        e2.cancel();

        Pageable pageable = PageRequest.of(0, 10);
        Page<MyEnrollmentListItem> page = enrollmentService.searchMyEnrollments(
                new SearchMyEnrollmentsCommand(
                        classmate.getId(), EnrollmentStatus.CANCELLED, null, null
                ),
                pageable
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).enrollmentId()).isEqualTo(e2.getId());
        assertThat(page.getContent().get(0).status()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void 정렬_CREATED_ASC() {
        LiveClass cls1 = createOpenClass(10);
        LiveClass cls2 = createOpenClass(10);
        Enrollment first = enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls1.getId()));
        Enrollment second = enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls2.getId()));

        Pageable pageable = PageRequest.of(0, 10);
        Page<MyEnrollmentListItem> page = enrollmentService.searchMyEnrollments(
                new SearchMyEnrollmentsCommand(
                        classmate.getId(), null, EnrollmentSortType.CREATED, SortDirection.ASC
                ),
                pageable
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).enrollmentId()).isEqualTo(first.getId());
        assertThat(page.getContent().get(1).enrollmentId()).isEqualTo(second.getId());
    }

    @Test
    void 페이징_size_1_total_3() {
        for (int i = 0; i < 3; i++) {
            LiveClass cls = createOpenClass(10);
            enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls.getId()));
        }

        Pageable pageable = PageRequest.of(0, 1);
        Page<MyEnrollmentListItem> page = enrollmentService.searchMyEnrollments(
                new SearchMyEnrollmentsCommand(classmate.getId(), null, null, null),
                pageable
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }
}
