package com.liveclass.course.controller.enrollment.response;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.service.ports.in.result.enrollment.MyEnrollmentListItem;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MyEnrollmentItemResponse(
        Long enrollmentId,
        Long classId,
        String classTitle,
        EnrollmentStatus status,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt
) {
    public static MyEnrollmentItemResponse from(MyEnrollmentListItem item) {
        return new MyEnrollmentItemResponse(
                item.enrollmentId(),
                item.classId(),
                item.classTitle(),
                item.status(),
                item.price(),
                item.createdAt(),
                item.paidAt(),
                item.cancelledAt()
        );
    }
}
