package com.liveclass.course.service;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class DefaultLiveClassService implements LiveClassService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final LiveClassRepository liveClassRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public LiveClass create(CreateClassCommand command) {
        validateDateRange(command);

        User creator = userRepository.findById(command.creatorId())
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        creator.assertCreator();

        LiveClass liveClass = LiveClass.builder()
                .title(command.title())
                .description(command.description())
                .price(command.price())
                .capacity(command.capacity())
                .startDate(command.startDate())
                .endDate(command.endDate())
                .creator(creator)
                .build();

        return liveClassRepository.save(liveClass);
    }

    @Override
    public ClassDetail get(Long classId) {
        LiveClass liveClass = liveClassRepository.findByIdWithCreator(classId)
                .orElseThrow(() -> new CustomException(ClassErrorCode.CLASS_NOT_FOUND));

        long currentEnrolled = enrollmentRepository
                .countByLiveClass_IdAndStatusIn(classId, ACTIVE_STATUSES);

        return new ClassDetail(liveClass, currentEnrolled);
    }

    private void validateDateRange(CreateClassCommand command) {
        if (!command.endDate().isAfter(command.startDate())) {
            throw new CustomException(
                    CommonErrorCode.INVALID_REQUEST,
                    "endDate는 startDate 이후여야 합니다.");
        }
    }
}
