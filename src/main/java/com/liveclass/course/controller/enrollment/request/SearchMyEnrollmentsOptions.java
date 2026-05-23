package com.liveclass.course.controller.enrollment.request;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.global.dto.EnrollmentSortType;
import com.liveclass.course.global.dto.SortDirection;
import com.liveclass.course.global.dto.request.PageOptions;
import com.liveclass.course.service.ports.in.command.enrollment.SearchMyEnrollmentsCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springdoc.core.annotations.ParameterObject;

@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ParameterObject
public class SearchMyEnrollmentsOptions extends PageOptions {

    private EnrollmentStatus status;
    private EnrollmentSortType sortType;
    private SortDirection sortDirection;

    public SearchMyEnrollmentsCommand toCommand(Long userId) {
        return new SearchMyEnrollmentsCommand(userId, status, sortType, sortDirection);
    }
}
