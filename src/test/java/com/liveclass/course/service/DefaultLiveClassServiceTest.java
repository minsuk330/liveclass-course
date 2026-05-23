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
import com.liveclass.course.global.error.CommonErrorCode;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.UserErrorCode;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.LiveClassRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.repository.WaitlistEntryRepository;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.global.dto.ClassSortType;
import com.liveclass.course.global.dto.SortDirection;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassEnrollmentsCommand;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassesCommand;
import com.liveclass.course.service.ports.in.result.liveclass.ClassDetail;
import com.liveclass.course.service.ports.in.result.liveclass.ClassListItem;
import com.liveclass.course.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class DefaultLiveClassServiceTest extends IntegrationTestBase {

    @Autowired
    private LiveClassService liveClassService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LiveClassRepository liveClassRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    @PersistenceContext
    private EntityManager entityManager;

    private User creator;
    private User classmate;

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

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

    private LiveClass createClass(String title) {
        return liveClassService.create(new CreateClassCommand(
                creator.getId(),
                title,
                null,
                new BigDecimal("10000"),
                10,
                LocalDate.now().plusDays(7),
                LocalDate.now().plusDays(30)
        ));
    }

    @Test
    void 강의_목록_조회_빈_결과() {
        Page<ClassListItem> page = liveClassService.search(
                new SearchClassesCommand(null, null, null, null),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void 강의_목록_조회_status_필터() {
        createClass("DRAFT 강의");
        LiveClass open = createClass("OPEN 강의");
        open.changeStatus(ClassStatus.OPEN);
        liveClassRepository.save(open);

        Page<ClassListItem> openOnly = liveClassService.search(
                new SearchClassesCommand(ClassStatus.OPEN, null, null, null),
                PageRequest.of(0, 10)
        );

        assertThat(openOnly.getTotalElements()).isEqualTo(1);
        assertThat(openOnly.getContent().get(0).liveClass().getTitle())
                .isEqualTo("OPEN 강의");
    }

    @Test
    void 강의_목록_조회_creatorId_필터() {
        User otherCreator = userRepository.save(
                User.builder().name("다른강사").role(UserRole.CREATOR).build()
        );
        createClass("내 강의 A");
        createClass("내 강의 B");
        liveClassService.create(new CreateClassCommand(
                otherCreator.getId(), "남의 강의", null,
                new BigDecimal("10000"), 10,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));

        Page<ClassListItem> mine = liveClassService.search(
                new SearchClassesCommand(null, creator.getId(), null, null),
                PageRequest.of(0, 10)
        );

        assertThat(mine.getTotalElements()).isEqualTo(2);
        assertThat(mine.getContent())
                .extracting(i -> i.liveClass().getCreator().getId())
                .containsOnly(creator.getId());
    }

    @Test
    void 강의_목록_조회_currentEnrolled_일괄_집계() {
        LiveClass c1 = createClass("강의1");
        LiveClass c2 = createClass("강의2");

        enrollmentRepository.save(Enrollment.builder()
                .liveClass(c1).user(classmate)
                .status(EnrollmentStatus.PENDING).build());
        enrollmentRepository.save(Enrollment.builder()
                .liveClass(c1).user(classmate)
                .status(EnrollmentStatus.CONFIRMED).build());
        enrollmentRepository.save(Enrollment.builder()
                .liveClass(c2).user(classmate)
                .status(EnrollmentStatus.CANCELLED).build());

        Page<ClassListItem> page = liveClassService.search(
                new SearchClassesCommand(null, null, null, null),
                PageRequest.of(0, 10, Sort.by("createdAt").ascending())
        );

        assertThat(page.getContent()).hasSize(2);
        ClassListItem item1 = page.getContent().stream()
                .filter(i -> i.liveClass().getId().equals(c1.getId()))
                .findFirst().orElseThrow();
        ClassListItem item2 = page.getContent().stream()
                .filter(i -> i.liveClass().getId().equals(c2.getId()))
                .findFirst().orElseThrow();
        assertThat(item1.currentEnrolled()).isEqualTo(2L);
        assertThat(item2.currentEnrolled()).isZero();
    }

    @Test
    void 정렬_화이트리스트_허용_안된_필드는_무시되고_default_적용() {
        LiveClass first = createClass("aaa");
        LiveClass second = createClass("bbb");

        Pageable maliciousSort = PageRequest.of(0, 10, Sort.by(
                Sort.Order.asc("password"),
                Sort.Order.asc("internalField")
        ));

        Page<ClassListItem> page = liveClassService.search(
                new SearchClassesCommand(null, null, null, null), maliciousSort
        );

        assertThat(page.getContent())
                .extracting(i -> i.liveClass().getId())
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void 페이지_크기_100_초과시_100으로_제한() {
        for (int i = 0; i < 3; i++) createClass("c" + i);

        Page<ClassListItem> page = liveClassService.search(
                new SearchClassesCommand(null, null, null, null),
                PageRequest.of(0, 999)
        );

        assertThat(page.getSize()).isEqualTo(100);
        assertThat(page.getContent()).hasSize(3);
    }

    @Test
    void 정렬_PRICE_ASC_enum_적용() {
        LiveClass cheap = liveClassService.create(new CreateClassCommand(
                creator.getId(), "싼강의", null, new BigDecimal("1000"), 10,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));
        LiveClass expensive = liveClassService.create(new CreateClassCommand(
                creator.getId(), "비싼강의", null, new BigDecimal("100000"), 10,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));

        Page<ClassListItem> page = liveClassService.search(
                new SearchClassesCommand(null, null, ClassSortType.PRICE, SortDirection.ASC),
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
                .extracting(i -> i.liveClass().getId())
                .containsExactly(cheap.getId(), expensive.getId());
    }

    @Test
    void 정렬_PRICE_DESC_enum_적용() {
        LiveClass cheap = liveClassService.create(new CreateClassCommand(
                creator.getId(), "싼강의", null, new BigDecimal("1000"), 10,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));
        LiveClass expensive = liveClassService.create(new CreateClassCommand(
                creator.getId(), "비싼강의", null, new BigDecimal("100000"), 10,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(30)
        ));

        Page<ClassListItem> page = liveClassService.search(
                new SearchClassesCommand(null, null, ClassSortType.PRICE, SortDirection.DESC),
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent())
                .extracting(i -> i.liveClass().getId())
                .containsExactly(expensive.getId(), cheap.getId());
    }

    @Test
    void 상태_전환_DRAFT_to_OPEN_성공() {
        LiveClass saved = createClass("강의1");

        liveClassService.changeStatus(new ChangeClassStatusCommand(
                saved.getId(), creator.getId(), ClassStatus.OPEN
        ));

        ClassDetail detail = liveClassService.get(saved.getId());
        assertThat(detail.liveClass().getStatus()).isEqualTo(ClassStatus.OPEN);
    }

    @Test
    void 상태_전환_OPEN_to_CLOSED_성공() {
        LiveClass saved = createClass("강의1");
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                saved.getId(), creator.getId(), ClassStatus.OPEN
        ));

        liveClassService.changeStatus(new ChangeClassStatusCommand(
                saved.getId(), creator.getId(), ClassStatus.CLOSED
        ));

        ClassDetail detail = liveClassService.get(saved.getId());
        assertThat(detail.liveClass().getStatus()).isEqualTo(ClassStatus.CLOSED);
    }

    @Test
    void 상태_전환_DRAFT_to_CLOSED_INVALID_STATUS_TRANSITION() {
        LiveClass saved = createClass("강의1");

        assertThatThrownBy(() -> liveClassService.changeStatus(new ChangeClassStatusCommand(
                saved.getId(), creator.getId(), ClassStatus.CLOSED
        )))
                .isInstanceOf(com.liveclass.course.domain.common.DomainException.class);
    }

    @Test
    void 상태_전환_존재하지_않는_강의_CLASS_NOT_FOUND() {
        assertThatThrownBy(() -> liveClassService.changeStatus(new ChangeClassStatusCommand(
                999_999L, creator.getId(), ClassStatus.OPEN
        )))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_NOT_FOUND);
    }

    @Test
    void 상태_전환_본인_강의_아니면_NOT_CLASS_OWNER() {
        LiveClass saved = createClass("강의1");
        User other = userRepository.save(
                User.builder().name("다른강사").role(UserRole.CREATOR).build()
        );

        assertThatThrownBy(() -> liveClassService.changeStatus(new ChangeClassStatusCommand(
                saved.getId(), other.getId(), ClassStatus.OPEN
        )))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.NOT_CLASS_OWNER);
    }

    @Test
    void 수강생_목록_조회_성공() {
        LiveClass saved = createClass("강의1");
        enrollmentRepository.save(Enrollment.builder()
                .liveClass(saved).user(classmate)
                .status(EnrollmentStatus.PENDING).build());

        Page<Enrollment> page = liveClassService.searchEnrollments(
                new SearchClassEnrollmentsCommand(saved.getId(), creator.getId(), null),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getUser().getId()).isEqualTo(classmate.getId());
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(EnrollmentStatus.PENDING);
    }

    @Test
    void 수강생_목록_조회_status_필터_CONFIRMED만() {
        LiveClass saved = createClass("강의1");
        User other = userRepository.save(
                User.builder().name("수강생2").role(UserRole.CLASSMATE).build()
        );

        enrollmentRepository.save(Enrollment.builder()
                .liveClass(saved).user(classmate)
                .status(EnrollmentStatus.PENDING).build());
        enrollmentRepository.save(Enrollment.builder()
                .liveClass(saved).user(other)
                .status(EnrollmentStatus.CONFIRMED).build());

        Page<Enrollment> confirmedOnly = liveClassService.searchEnrollments(
                new SearchClassEnrollmentsCommand(
                        saved.getId(), creator.getId(), EnrollmentStatus.CONFIRMED),
                PageRequest.of(0, 10)
        );

        assertThat(confirmedOnly.getTotalElements()).isEqualTo(1);
        assertThat(confirmedOnly.getContent().get(0).getUser().getId())
                .isEqualTo(other.getId());
    }

    @Test
    void 수강생_목록_조회_본인_강의_아니면_NOT_CLASS_OWNER() {
        LiveClass saved = createClass("강의1");
        User otherCreator = userRepository.save(
                User.builder().name("다른강사").role(UserRole.CREATOR).build()
        );

        assertThatThrownBy(() -> liveClassService.searchEnrollments(
                new SearchClassEnrollmentsCommand(saved.getId(), otherCreator.getId(), null),
                PageRequest.of(0, 10)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.NOT_CLASS_OWNER);
    }

    @Test
    void 수강생_목록_조회_존재하지_않는_강의_CLASS_NOT_FOUND() {
        assertThatThrownBy(() -> liveClassService.searchEnrollments(
                new SearchClassEnrollmentsCommand(999_999L, creator.getId(), null),
                PageRequest.of(0, 10)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_NOT_FOUND);
    }

    // ---------- delete ----------

    @Test
    void 강의_삭제_성공_soft_delete_적용() {
        LiveClass saved = createClass("강의1");

        liveClassService.delete(saved.getId(), creator.getId());
        flushAndClear();

        assertThat(liveClassRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void 강의_삭제_존재하지_않는_강의_CLASS_NOT_FOUND() {
        assertThatThrownBy(() -> liveClassService.delete(999_999L, creator.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_NOT_FOUND);
    }

    @Test
    void 강의_삭제_본인_강의_아니면_NOT_CLASS_OWNER() {
        LiveClass saved = createClass("강의1");
        User other = userRepository.save(
                User.builder().name("다른강사").role(UserRole.CREATOR).build()
        );

        assertThatThrownBy(() -> liveClassService.delete(saved.getId(), other.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.NOT_CLASS_OWNER);
    }

    @Test
    void 강의_삭제_활성_신청_있으면_CLASS_HAS_ACTIVE_ENROLLMENTS() {
        LiveClass saved = createClass("강의1");
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                saved.getId(), creator.getId(), ClassStatus.OPEN
        ));
        enrollmentService.create(new CreateEnrollmentCommand(classmate.getId(), saved.getId()));

        assertThatThrownBy(() -> liveClassService.delete(saved.getId(), creator.getId()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ClassErrorCode.CLASS_HAS_ACTIVE_ENROLLMENTS);
    }

    @Test
    void 강의_삭제_취소된_신청만_있으면_삭제_가능() {
        LiveClass saved = createClass("강의1");
        liveClassService.changeStatus(new ChangeClassStatusCommand(
                saved.getId(), creator.getId(), ClassStatus.OPEN
        ));
        Enrollment e = enrollmentService.create(
                new CreateEnrollmentCommand(classmate.getId(), saved.getId())
        );
        e.cancel();
        enrollmentRepository.save(e);

        liveClassService.delete(saved.getId(), creator.getId());
        flushAndClear();

        assertThat(liveClassRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void 강의_삭제_시_대기열_cascade_soft_delete() {
        LiveClass saved = createClass("강의1");
        WaitlistEntry w1 = waitlistEntryRepository.save(WaitlistEntry.builder()
                .liveClass(saved).user(classmate).position(1).build());
        User classmate2 = userRepository.save(
                User.builder().name("수강생2").role(UserRole.CLASSMATE).build()
        );
        WaitlistEntry w2 = waitlistEntryRepository.save(WaitlistEntry.builder()
                .liveClass(saved).user(classmate2).position(2).build());

        liveClassService.delete(saved.getId(), creator.getId());
        flushAndClear();

        assertThat(waitlistEntryRepository.findById(w1.getId())).isEmpty();
        assertThat(waitlistEntryRepository.findById(w2.getId())).isEmpty();
    }
}
