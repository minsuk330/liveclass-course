package com.liveclass.course.service.ports.in.command.enrollment;

public record CancelEnrollmentCommand(
        Long enrollmentId,
        Long userId
) {
}
