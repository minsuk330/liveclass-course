package com.liveclass.course.service.ports.in.command.liveclass;

import com.liveclass.course.domain.liveclass.ClassStatus;

public record ChangeClassStatusCommand(
        Long classId,
        Long creatorId,
        ClassStatus targetStatus
) {
}
