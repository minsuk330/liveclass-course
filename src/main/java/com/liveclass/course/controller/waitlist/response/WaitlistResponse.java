package com.liveclass.course.controller.waitlist.response;

import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import java.time.LocalDateTime;

public record WaitlistResponse(
        Long waitlistId,
        Long classId,
        Long userId,
        Integer position,
        LocalDateTime createdAt
) {
    public static WaitlistResponse from(WaitlistEntry w) {
        return new WaitlistResponse(
                w.getId(),
                w.getLiveClass().getId(),
                w.getUser().getId(),
                w.getPosition(),
                w.getCreatedAt()
        );
    }
}
