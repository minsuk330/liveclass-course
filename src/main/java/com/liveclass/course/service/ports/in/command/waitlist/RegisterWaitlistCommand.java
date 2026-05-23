package com.liveclass.course.service.ports.in.command.waitlist;

public record RegisterWaitlistCommand(
        Long classId,
        Long userId
) {
}
