package com.liveclass.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.domain.user.UserRole;
import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import com.liveclass.course.global.error.ClassErrorCode;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.UserErrorCode;
import com.liveclass.course.global.error.WaitlistErrorCode;
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
import com.liveclass.course.service.ports.in.command.waitlist.CancelWaitlistCommand;
import com.liveclass.course.service.ports.in.command.waitlist.RegisterWaitlistCommand;
import com.liveclass.course.service.ports.in.result.waitlist.MyWaitlistListItem;
import com.liveclass.course.support.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class DefaultWaitlistServiceTest extends IntegrationTestBase {

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

    @PersistenceContext
    private EntityManager entityManager;

    private User creator;
    private User classmate;
    private User other1;
    private User other2;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @BeforeEach
    void seed() {
        creator = userRepository.save(User.builder().name("강사").role(UserRole.CREATOR).build());
        classmate = userRepository.save(User.builder().name("수강생").role(UserRole.CLASSMATE).build());
        other1 = userRepository.save(User.builder().name("수강생1").role(UserRole.CLASSMATE).build());
        other2 = userRepository.save(User.builder().name("수강생2").role(UserRole.CLASSMATE).build());
    }

    private LiveClass createOpenClass(int capacity) {
        LiveClass cls = liveClassService.create(new CreateClassCommand(
                creator.getId(), "강의", null, new BigDecimal("10000"), capacity,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                cls.getId(), creator.getId(), ClassStatus.OPEN
        ));
        return cls;
    }

    private LiveClass createFullClass() {
        LiveClass cls = createOpenClass(1);
        enrollmentService.create(new CreateEnrollmentCommand(other1.getId(), cls.getId()));
        return cls;
    }

    // ---------- register ----------

    @Test
    void 대기열_등록_성공_position_1() {
        LiveClass cls = createFullClass();

        WaitlistEntry entry = waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        );

        assertThat(entry.getPosition()).isEqualTo(1);
        assertThat(entry.getUser().getId()).isEqualTo(classmate.getId());
    }

    @Test
    void 대기열_두번째_등록은_position_2() {
        LiveClass cls = createFullClass();
        waitlistService.register(new RegisterWaitlistCommand(cls.getId(), classmate.getId()));

        WaitlistEntry second = waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), other2.getId())
        );

        assertThat(second.getPosition()).isEqualTo(2);
    }

    @Test
    void 정원_안_찼으면_CLASS_NOT_FULL() {
        LiveClass cls = createOpenClass(10);

        assertThatThrownBy(() -> waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(WaitlistErrorCode.CLASS_NOT_FULL);
    }

    @Test
    void OPEN_아니면_CLASS_NOT_OPEN() {
        LiveClass cls = liveClassService.create(new CreateClassCommand(
                creator.getId(), "DRAFT", null, new BigDecimal("10000"), 1,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));

        assertThatThrownBy(() -> waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_NOT_OPEN);
    }

    @Test
    void 활성_신청자는_대기열_등록_불가_ENROLLMENT_ALREADY_ACTIVE() {
        LiveClass cls = createOpenClass(10);
        enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), cls.getId()));
        // 정원 충족 강제하려고 추가 신청
        for (int i = 0; i < 9; i++) {
            User u = userRepository.save(User.builder().name("u" + i).role(UserRole.CLASSMATE).build());
            enrollmentService.create(new CreateEnrollmentCommand(u.getId(), cls.getId()));
        }

        assertThatThrownBy(() -> waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(WaitlistErrorCode.ENROLLMENT_ALREADY_ACTIVE);
    }

    @Test
    void 중복_대기열_등록_WAITLIST_DUPLICATE() {
        LiveClass cls = createFullClass();
        waitlistService.register(new RegisterWaitlistCommand(cls.getId(), classmate.getId()));

        assertThatThrownBy(() -> waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(WaitlistErrorCode.WAITLIST_DUPLICATE);
    }

    // ---------- searchMyWaitlist ----------

    @Test
    void 내_대기열_목록_classTitle_포함() {
        LiveClass cls = createFullClass();
        waitlistService.register(new RegisterWaitlistCommand(cls.getId(), classmate.getId()));

        Page<MyWaitlistListItem> page = waitlistService.searchMyWaitlist(
                classmate.getId(), PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        MyWaitlistListItem item = page.getContent().get(0);
        assertThat(item.classId()).isEqualTo(cls.getId());
        assertThat(item.classTitle()).isEqualTo("강의");
        assertThat(item.position()).isEqualTo(1);
    }

    @Test
    void 내_대기열_빈_결과() {
        Page<MyWaitlistListItem> page = waitlistService.searchMyWaitlist(
                classmate.getId(), PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void 내_대기열_사용자_없으면_USER_NOT_FOUND() {
        assertThatThrownBy(() -> waitlistService.searchMyWaitlist(
                999_999L, PageRequest.of(0, 10)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    // ---------- cancel ----------

    @Test
    void 대기열_이탈_성공_soft_delete() {
        LiveClass cls = createFullClass();
        WaitlistEntry entry = waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        );

        waitlistService.cancel(new CancelWaitlistCommand(entry.getId(), classmate.getId()));
        flushAndClear();

        assertThat(waitlistEntryRepository.findById(entry.getId())).isEmpty();
    }

    @Test
    void 대기열_이탈_본인_아니면_FORBIDDEN_WAITLIST_ACCESS() {
        LiveClass cls = createFullClass();
        WaitlistEntry entry = waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        );

        assertThatThrownBy(() -> waitlistService.cancel(
                new CancelWaitlistCommand(entry.getId(), other2.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(WaitlistErrorCode.FORBIDDEN_WAITLIST_ACCESS);
    }

    @Test
    void 대기열_이탈_없는_entryId_WAITLIST_NOT_FOUND() {
        assertThatThrownBy(() -> waitlistService.cancel(
                new CancelWaitlistCommand(999_999L, classmate.getId())
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(WaitlistErrorCode.WAITLIST_NOT_FOUND);
    }

    @Test
    void 이탈_후_같은_강의에_재등록_가능() {
        LiveClass cls = createFullClass();
        WaitlistEntry first = waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        );
        waitlistService.cancel(new CancelWaitlistCommand(first.getId(), classmate.getId()));
        flushAndClear();

        WaitlistEntry rejoined = waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        );

        assertThat(rejoined.getId()).isNotEqualTo(first.getId());
        assertThat(rejoined.getPosition()).isEqualTo(2); // max(1) + 1, 이탈해도 position 보존
    }

    // ---------- auto-promote ----------

    @Test
    void CONFIRMED_취소시_대기열_1번_자동_PENDING_승격() {
        LiveClass cls = createOpenClass(1);
        Enrollment paid = enrollmentService.create(
                new CreateEnrollmentCommand(other1.getId(), cls.getId())
        );
        paid.confirmPayment();
        enrollmentRepository.save(paid);
        flushAndClear();

        WaitlistEntry waiting = waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        );

        enrollmentService.cancel(new CancelEnrollmentCommand(paid.getId(), other1.getId()));
        flushAndClear();

        // waitlist 1번 soft delete
        assertThat(waitlistEntryRepository.findById(waiting.getId())).isEmpty();
        // classmate enrollment PENDING 신규 생성
        Page<Enrollment> myEnrollments = enrollmentRepository.searchByClass(
                cls.getId(), EnrollmentStatus.PENDING, PageRequest.of(0, 10)
        );
        assertThat(myEnrollments.getContent())
                .extracting(e -> e.getUser().getId())
                .contains(classmate.getId());
    }

    @Test
    void PENDING_취소는_승격_트리거_안함() {
        LiveClass cls = createOpenClass(1);
        Enrollment pending = enrollmentService.create(
                new CreateEnrollmentCommand(other1.getId(), cls.getId())
        );
        WaitlistEntry waiting = waitlistService.register(
                new RegisterWaitlistCommand(cls.getId(), classmate.getId())
        );

        enrollmentService.cancel(new CancelEnrollmentCommand(pending.getId(), other1.getId()));
        flushAndClear();

        assertThat(waitlistEntryRepository.findById(waiting.getId())).isPresent();
    }

    @Test
    void 대기열_없으면_promote_skip() {
        LiveClass cls = createOpenClass(1);
        Enrollment paid = enrollmentService.create(
                new CreateEnrollmentCommand(other1.getId(), cls.getId())
        );
        paid.confirmPayment();
        enrollmentRepository.save(paid);
        flushAndClear();

        enrollmentService.cancel(new CancelEnrollmentCommand(paid.getId(), other1.getId()));
        flushAndClear();

        long active = enrollmentRepository.countByLiveClass_IdAndStatusIn(
                cls.getId(),
                java.util.List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED)
        );
        assertThat(active).isZero();
    }

    @Test
    void 대기열_취소_후_재등록() {

    }

    // ---------- CLASS_CAPACITY_EXCEEDED hint ----------

    @Test
    void 정원_초과_시_meta에_waitlist_정보_포함() {
        LiveClass cls = createFullClass();
        waitlistService.register(new RegisterWaitlistCommand(cls.getId(), classmate.getId()));
        waitlistService.register(new RegisterWaitlistCommand(cls.getId(), other2.getId()));

        User newUser = userRepository.save(
                User.builder().name("새신청자").role(UserRole.CLASSMATE).build()
        );

        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> enrollmentService.create(
                new CreateEnrollmentCommand(newUser.getId(), cls.getId())
        ))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> {
                    CustomException ex = (CustomException) e;
                    assertThat(ex.getErrorCode()).isEqualTo(ClassErrorCode.CLASS_CAPACITY_EXCEEDED);
                    assertThat(ex.getMeta()).isNotNull();
                    assertThat(ex.getMeta().get("waitlistAvailable")).isEqualTo(true);
                    assertThat(ex.getMeta().get("currentWaitlistSize")).isEqualTo(2L);
                    assertThat(ex.getMeta().get("waitlistEndpoint"))
                            .isEqualTo("/api/v1/classes/" + cls.getId() + "/waitlist");
                });
    }
}
