package com.liveclass.course.service.ports.in.command.liveclass;

import com.liveclass.course.domain.liveclass.ClassStatus;
import com.liveclass.course.global.dto.SortDirection;
import com.liveclass.course.global.dto.ClassSortType;

public record SearchClassesCommand(
        ClassStatus status,
        Long creatorId,
        ClassSortType classSortType,
        SortDirection sortDirection
) {
}
