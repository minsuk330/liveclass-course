package com.liveclass.course.service.ports.in.result.liveclass;

import com.liveclass.course.domain.liveclass.LiveClass;

public record ClassDetail(
        LiveClass liveClass,
        long currentEnrolled
) {
}
