package com.liveclass.course.service.ports.in.command.enrollment;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.global.dto.EnrollmentSortType;
import com.liveclass.course.global.dto.SortDirection;

public record SearchMyEnrollmentsCommand(
        Long userId,
        EnrollmentStatus status,
        EnrollmentSortType sortType,
        SortDirection sortDirection
) {
}
