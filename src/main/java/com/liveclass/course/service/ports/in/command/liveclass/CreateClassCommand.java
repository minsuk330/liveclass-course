package com.liveclass.course.service.ports.in.command.liveclass;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateClassCommand(
        Long creatorId,
        String title,
        String description,
        BigDecimal price,
        Integer capacity,
        LocalDate startDate,
        LocalDate endDate
) {
}
