package com.liveclass.course.service.ports.in.result.waitlist;

import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import java.time.LocalDateTime;

public record MyWaitlistListItem(
        Long waitlistId,
        Long classId,
        String classTitle,
        Integer position,
        LocalDateTime createdAt
) {
    public static MyWaitlistListItem from(WaitlistEntry w) {
        return new MyWaitlistListItem(
                w.getId(),
                w.getLiveClass().getId(),
                w.getLiveClass().getTitle(),
                w.getPosition(),
                w.getCreatedAt()
        );
    }
}
