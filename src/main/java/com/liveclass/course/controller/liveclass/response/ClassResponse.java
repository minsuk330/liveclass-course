package com.liveclass.course.controller.liveclass.response;

import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.domain.liveclass.LiveClass;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClassResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Integer capacity,
        LocalDate startDate,
        LocalDate endDate,
        ClassStatus status,
        Long creatorId,
        long currentEnrolled,
        long availableSeats,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ClassResponse from(LiveClass cls, long currentEnrolled) {
        long available = Math.max(0L, cls.getCapacity() - currentEnrolled);
        return new ClassResponse(
                cls.getId(),
                cls.getTitle(),
                cls.getDescription(),
                cls.getPrice(),
                cls.getCapacity(),
                cls.getStartDate(),
                cls.getEndDate(),
                cls.getStatus(),
                cls.getCreator().getId(),
                currentEnrolled,
                available,
                cls.getCreatedAt(),
                cls.getUpdatedAt()
        );
    }
}
