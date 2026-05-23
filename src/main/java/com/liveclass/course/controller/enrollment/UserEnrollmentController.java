package com.liveclass.course.controller.enrollment;

import com.liveclass.course.controller.enrollment.request.SearchMyEnrollmentsOptions;
import com.liveclass.course.controller.enrollment.response.MyEnrollmentItemResponse;
import com.liveclass.course.global.dto.response.PageResponse;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.result.enrollment.MyEnrollmentListItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/enrollments")
@RequiredArgsConstructor
@Validated
@Tag(name = "내 수강 신청", description = "사용자별 수강 신청 조회 API")
public class UserEnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @Operation(
            summary = "내 수강 신청 목록 조회",
            description = "사용자의 수강 신청 목록을 페이지네이션 + 필터 + 정렬로 조회합니다."
    )
    public ResponseEntity<PageResponse<MyEnrollmentItemResponse>> searchMyEnrollments(
            @PathVariable Long userId,
            @ModelAttribute SearchMyEnrollmentsOptions options
    ) {
        Page<MyEnrollmentListItem> page = enrollmentService.searchMyEnrollments(
                options.toCommand(userId), options.pageable()
        );
        return ResponseEntity.ok(PageResponse.of(page, MyEnrollmentItemResponse::from));
    }
}
