package com.liveclass.course.controller.waitlist.response;

import com.liveclass.course.service.ports.in.result.waitlist.MyWaitlistListItem;
import java.time.LocalDateTime;

public record MyWaitlistItemResponse(
        Long waitlistId,
        Long classId,
        String classTitle,
        Integer position,
        LocalDateTime createdAt
) {
    public static MyWaitlistItemResponse from(MyWaitlistListItem item) {
        return new MyWaitlistItemResponse(
                item.waitlistId(),
                item.classId(),
                item.classTitle(),
                item.position(),
                item.createdAt()
        );
    }
}
