package com.liveclass.course.service;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import com.liveclass.course.global.error.ClassErrorCode;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.UserErrorCode;
import com.liveclass.course.global.error.WaitlistErrorCode;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.LiveClassRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.repository.WaitlistEntryRepository;
import com.liveclass.course.service.ports.in.WaitlistService;
import com.liveclass.course.service.ports.in.command.waitlist.CancelWaitlistCommand;
import com.liveclass.course.service.ports.in.command.waitlist.RegisterWaitlistCommand;
import com.liveclass.course.service.ports.in.result.waitlist.MyWaitlistListItem;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class DefaultWaitlistService implements WaitlistService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final LiveClassRepository liveClassRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public WaitlistEntry register(RegisterWaitlistCommand command) {
        // 비관적 락 첫 read
        LiveClass liveClass = liveClassRepository.findByIdForUpdate(command.classId())
                .orElseThrow(() -> new CustomException(ClassErrorCode.CLASS_NOT_FOUND));

        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        if (liveClass.getStatus() != ClassStatus.OPEN) {
            throw new CustomException(ClassErrorCode.CLASS_NOT_OPEN);
        }

        long active = enrollmentRepository
                .countByLiveClass_IdAndStatusIn(command.classId(), ACTIVE_STATUSES);

        if (waitlistEntryRepository.existsByLiveClass_IdAndUser_Id(
            command.classId(), command.userId())) {
          throw new CustomException(WaitlistErrorCode.WAITLIST_DUPLICATE);
        }

        if (active < liveClass.getCapacity()) {
          throw new CustomException(WaitlistErrorCode.CLASS_NOT_FULL,
              Map.of(
                  "enrollmentEndpoint", "/api/v1/enrollments?userId={userId}",
                  "method", "POST",
                  "classId", command.classId()
              ));
        }

        if (enrollmentRepository.existsByLiveClass_IdAndUser_IdAndStatusIn(
                command.classId(), command.userId(), ACTIVE_STATUSES)) {
            throw new CustomException(WaitlistErrorCode.ENROLLMENT_ALREADY_ACTIVE);
        }

        int nextPosition = waitlistEntryRepository
                .findMaxPositionByLiveClassId(command.classId()) + 1;

        WaitlistEntry entry = WaitlistEntry.builder()
            .liveClass(liveClass)
            .user(user)
            .activeUserId(command.userId())
            .position(nextPosition)
            .build();
        return waitlistEntryRepository.save(entry);
    }

    @Override
    public Page<MyWaitlistListItem> searchMyWaitlist(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }
        return waitlistEntryRepository.findByUserIdWithClass(userId, pageable)
                .map(MyWaitlistListItem::from);
    }

    @Override
    @Transactional
    public void cancel(CancelWaitlistCommand command) {
        WaitlistEntry entry = waitlistEntryRepository.findById(command.entryId())
                .orElseThrow(() -> new CustomException(WaitlistErrorCode.WAITLIST_NOT_FOUND));

        if (!entry.getUser().getId().equals(command.userId())) {
            throw new CustomException(WaitlistErrorCode.FORBIDDEN_WAITLIST_ACCESS);
        }

        entry.softDelete();
    }
}
