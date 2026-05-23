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
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.LiveClassRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.command.enrollment.CancelEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import com.liveclass.course.service.ports.in.command.enrollment.SearchMyEnrollmentsCommand;
import com.liveclass.course.service.ports.in.result.enrollment.MyEnrollmentListItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
            throw new CustomException(ClassErrorCode.CLASS_CAPACITY_EXCEEDED);
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

    @Override
    @Transactional
    public void cancel(CancelEnrollmentCommand command) {
        // NOTE: 본인만 호출 가능 + 같은 enrollment 동시 cancel은 도메인 검증으로 idempotent
        // Waitlist promote 구현 시 LiveClass 비관적 락 필요 (lock-ordering: 메서드 첫 read여야 함)
        Enrollment enrollment = enrollmentRepository.findById(command.enrollmentId())
                .orElseThrow(() -> new CustomException(EnrollmentErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getUser().getId().equals(command.userId())) {
            throw new CustomException(EnrollmentErrorCode.FORBIDDEN_ENROLLMENT_ACCESS);
        }

        boolean wasConfirmed = enrollment.getStatus() == EnrollmentStatus.CONFIRMED;
        enrollment.cancel();

        if (wasConfirmed) {
            promoteFromWaitlist(enrollment.getLiveClass().getId());
        }
    }

    /**
     * CONFIRMED 취소로 자리 비면 대기열 1번을 PENDING으로 승격.
     * Waitlist API 미구현 — TODO: WaitlistService 도입 후 연결.
     * 본 구현 시 cancel() 진입부에 LiveClassRepository.findByIdForUpdate 추가 필요.
     */
    private void promoteFromWaitlist(Long classId) {
        // TODO: 대기열 1번 조회 → soft delete + Enrollment(PENDING) 생성
    }
}
