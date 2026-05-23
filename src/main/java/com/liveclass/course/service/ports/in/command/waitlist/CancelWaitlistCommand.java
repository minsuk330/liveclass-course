package com.liveclass.course.service.ports.in.command.waitlist;

public record CancelWaitlistCommand(
        Long entryId,
        Long userId
) {
}
