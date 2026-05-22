package com.liveclass.course.controller.enrollment.request;

import com.liveclass.course.service.ports.in.command.enrollment.CreateEnrollmentCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreateEnrollmentRequest(
        @NotNull
        @Schema(description = "수강 신청할 강의 ID", example = "101")
        Long classId
) {
    public CreateEnrollmentCommand toCommand(Long userId) {
        return new CreateEnrollmentCommand(userId, classId);
    }
}
