package com.liveclass.course.controller.liveclass.request;

import com.liveclass.course.domain.enrollment.EnrollmentStatus;
import com.liveclass.course.global.dto.request.PageOptions;
import com.liveclass.course.service.ports.in.command.liveclass.SearchClassEnrollmentsCommand;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class SearchClassEnrollmentsOptions extends PageOptions {

    @Schema(description = "수강 신청 상태 필터", example = "CONFIRMED")
    private EnrollmentStatus status;

    public SearchClassEnrollmentsCommand toCommand(Long classId, Long creatorId) {
        return new SearchClassEnrollmentsCommand(classId, creatorId, status);
    }
}
