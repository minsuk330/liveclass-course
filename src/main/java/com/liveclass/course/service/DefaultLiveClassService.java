package com.liveclass.course.service;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.domain.user.User;
import com.liveclass.course.global.error.ClassErrorCode;
import com.liveclass.course.global.error.CommonErrorCode;
import com.liveclass.course.global.error.CustomException;
import com.liveclass.course.global.error.UserErrorCode;
import com.liveclass.course.repository.EnrollmentRepository;
import com.liveclass.course.repository.LiveClassRepository;
import com.liveclass.course.repository.UserRepository;
import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassEnrollmentsCommand;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassesCommand;
import com.liveclass.course.service.ports.in.result.liveclass.ClassDetail;
import com.liveclass.course.service.ports.in.result.liveclass.ClassListItem;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class DefaultLiveClassService implements LiveClassService {

    private static final List<EnrollmentStatus> ACTIVE_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private static final int MAX_PAGE_SIZE = 100;

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

    @Override
    public Page<ClassListItem> search(SearchClassesCommand command, Pageable pageable) {
        Pageable safe = sanitize(pageable);

        Page<LiveClass> page = liveClassRepository.search(command, safe);

        if (page.isEmpty()) {
            return page.map(c -> new ClassListItem(c, 0L));
        }

        List<Long> ids = page.getContent().stream().map(LiveClass::getId).toList();
        Map<Long, Long> countMap = enrollmentRepository
                .countActiveByLiveClassIds(ids, ACTIVE_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));

        return page.map(c -> new ClassListItem(c, countMap.getOrDefault(c.getId(), 0L)));
    }

    @Override
    @Transactional
    public void changeStatus(ChangeClassStatusCommand command) {
        LiveClass liveClass = liveClassRepository.findByIdWithCreator(command.classId())
                .orElseThrow(() -> new CustomException(ClassErrorCode.CLASS_NOT_FOUND));

        if (!liveClass.getCreator().getId().equals(command.creatorId())) {
            throw new CustomException(ClassErrorCode.NOT_CLASS_OWNER);
        }

        liveClass.changeStatus(command.targetStatus());

    }

    @Override
    public Page<Enrollment> searchEnrollments(
            SearchClassEnrollmentsCommand command, Pageable pageable) {

        LiveClass liveClass = liveClassRepository.findByIdWithCreator(command.classId())
                .orElseThrow(() -> new CustomException(ClassErrorCode.CLASS_NOT_FOUND));

        if (!liveClass.getCreator().getId().equals(command.creatorId())) {
            throw new CustomException(ClassErrorCode.NOT_CLASS_OWNER);
        }

        Pageable safe = sanitize(pageable);
        return enrollmentRepository.searchByClass(command.classId(), command.status(), safe);
    }

    private Pageable sanitize(Pageable raw) {
        int size = Math.min(Math.max(raw.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(raw.getPageNumber(), size, raw.getSort());
    }

    private void validateDateRange(CreateClassCommand command) {
        if (!command.endDate().isAfter(command.startDate())) {
            throw new CustomException(
                    CommonErrorCode.INVALID_REQUEST,
                    "endDate는 startDate 이후여야 합니다.");
        }
    }
}
