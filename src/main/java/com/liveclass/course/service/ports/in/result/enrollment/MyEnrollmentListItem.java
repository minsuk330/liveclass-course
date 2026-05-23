package com.liveclass.course.service.ports.in.result.enrollment;

import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyEnrollmentListItem(
        Long enrollmentId,
        Long classId,
        String classTitle,
        EnrollmentStatus status,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt
) {
    public static MyEnrollmentListItem from(Enrollment e) {
        return new MyEnrollmentListItem(
                e.getId(),
                e.getLiveClass().getId(),
                e.getLiveClass().getTitle(),
                e.getStatus(),
                e.getLiveClass().getPrice(),
                e.getCreatedAt(),
                e.getPaidAt(),
                e.getCancelledAt()
        );
    }
}
