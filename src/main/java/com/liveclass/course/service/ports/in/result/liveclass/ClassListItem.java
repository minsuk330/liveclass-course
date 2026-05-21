package com.liveclass.course.service.ports.in.result.liveclass;

import com.liveclass.course.domain.liveclass.LiveClass;

public record ClassListItem(
        LiveClass liveClass,
        long currentEnrolled
) {
}
