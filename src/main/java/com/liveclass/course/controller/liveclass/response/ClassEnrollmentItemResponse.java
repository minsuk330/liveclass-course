package com.liveclass.course.controller.liveclass.response;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import java.time.LocalDateTime;

public record ClassEnrollmentItemResponse(
        Long enrollmentId,
        Long userId,
        String userName,
        EnrollmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt
) {
    public static ClassEnrollmentItemResponse from(Enrollment e) {
        return new ClassEnrollmentItemResponse(
                e.getId(),
                e.getUser().getId(),
                e.getUser().getName(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getPaidAt(),
                e.getCancelledAt()
        );
    }
}
