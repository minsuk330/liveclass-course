package com.liveclass.course.service;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.global.error.ClassErrorCode;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.EnrollmentErrorCode;
import com.liveclass.course.global.error.UserErrorCode;
import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.LiveClassRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.repository.WaitlistEntryRepository;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.command.enrollment.CancelEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.enrollment.SearchMyEnrollmentsCommand;
import com.liveclass.course.service.ports.in.result.enrollment.MyEnrollmentListItem;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class DefaultEnrollmentService implements EnrollmentService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final LiveClassRepository liveClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;

    @Override
    @Transactional
    public Enrollment create(CreateEnrollmentCommand command) {
        LiveClass liveClass = liveClassRepository.findByIdForUpdate(command.classId())
                .orElseThrow(() -> new CustomException(ClassErrorCode.CLASS_NOT_FOUND));

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (liveClass.getStatus() != ClassStatus.OPEN) {
            throw new CustomException(ClassErrorCode.CLASS_NOT_OPEN);
        }

        if (enrollmentRepository.existsByLiveClass_IdAndUser_IdAndStatusIn(
                command.classId(), command.userId(), ACTIVE_STATUSES)) {
            throw new CustomException(EnrollmentErrorCode.DUPLICATE_ENROLLMENT);
        }

        long current = enrollmentRepository
                .countByLiveClass_IdAndStatusIn(command.classId(), ACTIVE_STATUSES);
        if (current >= liveClass.getCapacity()) {
            long waitlistSize = waitlistEntryRepository.countByLiveClass_Id(command.classId());
            throw new CustomException(
                    ClassErrorCode.CLASS_CAPACITY_EXCEEDED,
                    Map.of(
                            "waitlistAvailable", true,
                            "currentWaitlistSize", waitlistSize,
                            "waitlistEndpoint", "/api/v1/classes/" + command.classId() + "/waitlist"
                    )
            );
        }

        Enrollment enrollment = Enrollment.builder()
                .liveClass(liveClass)
                .user(user)
                .build();
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public Page<MyEnrollmentListItem> searchMyEnrollments(
            SearchMyEnrollmentsCommand command, Pageable pageable
    ) {
        if (!userRepository.existsById(command.userId())) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }
        return enrollmentRepository.searchByUser(command, pageable)
                .map(MyEnrollmentListItem::from);
    }

    /**
     * READ_COMMITTED 사용 이유:
     * - enrollment를 먼저 조회해 classId를 알아내야 함 (락은 그 후 가능)
     * - REPEATABLE_READ면 첫 read가 snapshot 고정 → 락 후 waitlist 조회가 stale → 동시 cancel 시 같은 waitlist 중복 promote
     * - READ_COMMITTED는 매 SELECT마다 최신 데이터 → 락 직후 waitlist read도 다른 트랜잭션 commit 반영
     * 참조: [[concurrency-lock-ordering]]
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(CancelEnrollmentCommand command) {
        Enrollment enrollment = enrollmentRepository.findById(command.enrollmentId())
                .orElseThrow(() -> new CustomException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getUser().getId().equals(command.userId())) {
            throw new CustomException(EnrollmentErrorCode.FORBIDDEN_ENROLLMENT_ACCESS);
        }

        Long classId = enrollment.getLiveClass().getId();
        LiveClass liveClass = liveClassRepository.findByIdForUpdate(classId)
                .orElseThrow(() -> new CustomException(ClassErrorCode.CLASS_NOT_FOUND));

        boolean wasConfirmed = enrollment.getStatus() == EnrollmentStatus.CONFIRMED;
        enrollment.cancel();

        if (wasConfirmed) {
            promoteFromWaitlist(liveClass);
        }
    }

    /**
     * CONFIRMED 취소로 자리 비면 대기열 1번을 PENDING으로 승격.
     * 같은 트랜잭션에서 실행되어 부분 실패 시 전체 롤백.
     */
    private void promoteFromWaitlist(LiveClass liveClass) {
        WaitlistEntry next = waitlistEntryRepository
                .findFirstByLiveClass_IdOrderByPositionAsc(liveClass.getId())
                .orElse(null);
        if (next == null) {
            return;
        }
        User waitingUser = next.getUser();
        next.softDelete();

        Enrollment promoted = Enrollment.builder()
                .liveClass(liveClass)
                .user(waitingUser)
                .build();
        enrollmentRepository.save(promoted);
    }
}
