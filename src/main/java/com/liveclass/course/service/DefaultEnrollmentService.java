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
import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
}
