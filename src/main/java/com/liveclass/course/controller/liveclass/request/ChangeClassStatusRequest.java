package com.liveclass.course.controller.liveclass.request;

import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.service.ports.in.command.liveclass.ChangeClassStatusCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeClassStatusRequest(
        @NotNull
        @Schema(description = "전환할 상태", example = "OPEN")
        ClassStatus status
) {
    public ChangeClassStatusCommand toCommand(Long classId, Long creatorId) {
        return new ChangeClassStatusCommand(classId, creatorId, status);
    }
}
