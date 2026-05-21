package com.liveclass.course.service.ports.in.command.liveclass;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;

public record SearchClassEnrollmentsCommand(
        Long classId,
        Long creatorId,
        EnrollmentStatus status
) {
}
