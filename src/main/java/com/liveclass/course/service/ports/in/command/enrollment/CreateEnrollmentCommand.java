package com.liveclass.course.service.ports.in.command.enrollment;

public record CreateEnrollmentCommand(
        Long userId,
        Long classId
) {
}
