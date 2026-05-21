package com.liveclass.course.controller.liveclass.response;

import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.service.ports.in.result.liveclass.ClassListItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClassListItemResponse(
        Long id,
        String title,
        BigDecimal price,
        Integer capacity,
        LocalDate startDate,
        LocalDate endDate,
        ClassStatus status,
        Long creatorId,
        long currentEnrolled,
        long availableSeats,
        LocalDateTime createdAt
) {
    public static ClassListItemResponse from(ClassListItem item) {
        LiveClass cls = item.liveClass();
        long available = Math.max(0L, cls.getCapacity() - item.currentEnrolled());
        return new ClassListItemResponse(
                cls.getId(),
                cls.getTitle(),
                cls.getPrice(),
                cls.getCapacity(),
                cls.getStartDate(),
                cls.getEndDate(),
                cls.getStatus(),
                cls.getCreator().getId(),
                item.currentEnrolled(),
                available,
                cls.getCreatedAt()
        );
    }
}
