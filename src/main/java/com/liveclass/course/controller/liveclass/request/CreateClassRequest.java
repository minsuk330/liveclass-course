package com.liveclass.course.controller.liveclass.request;

import com.liveclass.course.service.ports.in.command.liveclass.CreateClassCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateClassRequest(
        @NotBlank
        @Size(max = 200)
        String title,

        String description,

        @NotNull
        @DecimalMin(value = "0")
        BigDecimal price,

        @NotNull
        @Min(1)
        Integer capacity,

        @NotNull
        @FutureOrPresent
        LocalDate startDate,

        @NotNull
        LocalDate endDate
) {
    public CreateClassCommand toCommand(Long creatorId) {
        return new CreateClassCommand(
                creatorId,
                title,
                description,
                price,
                capacity,
                startDate,
                endDate
        );
    }
}
