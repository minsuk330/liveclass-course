package com.liveclass.course.controller.enrollment.response;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long enrollmentId,
        Long classId,
        Long userId,
        EnrollmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt
) {
    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(
                e.getId(),
                e.getLiveClass().getId(),
                e.getUser().getId(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getPaidAt(),
                e.getCancelledAt()
        );
    }
}
