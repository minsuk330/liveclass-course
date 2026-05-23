package com.liveclass.course.controller.enrollment;

import com.liveclass.course.controller.enrollment.request.CreateEnrollmentRequest;
import com.liveclass.course.controller.enrollment.request.SearchMyEnrollmentsOptions;
import com.liveclass.course.controller.enrollment.response.EnrollmentResponse;
import com.liveclass.course.controller.enrollment.response.MyEnrollmentItemResponse;
import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.global.dto.response.PageResponse;
import com.liveclass.course.service.ports.in.EnrollmentService;
import com.liveclass.course.service.ports.in.command.enrollment.CancelEnrollmentCommand;
import com.liveclass.course.service.ports.in.result.enrollment.MyEnrollmentListItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "[회원]수강 신청", description = "수강 신청 관련 API")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/enrollment")
    @Operation(
            summary = "수강 신청",
            description = "OPEN 상태인 강의에 PENDING 상태로 신청합니다. 비관적 락으로 정원 초과 방지."
    )
    public ResponseEntity<EnrollmentResponse> create(
            @RequestParam @NotNull Long userId,
            @Valid @RequestBody CreateEnrollmentRequest request
    ) {
        Enrollment enrollment = enrollmentService.create(request.toCommand(userId));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(EnrollmentResponse.from(enrollment));
    }

    @GetMapping("/user/{userId}/enrollment")
    @Operation(
        summary = "수강 신청 목록 조회",
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


    @DeleteMapping("/enrollment/{enrollmentId}")
    @Operation(
            summary = "수강 취소",
            description = "PENDING은 즉시, CONFIRMED는 결제 후 7일 이내만 취소 가능합니다."
    )
    public ResponseEntity<Void> cancel(
            @PathVariable Long enrollmentId,
            @RequestParam @NotNull Long userId
    ) {
        enrollmentService.cancel(new CancelEnrollmentCommand(enrollmentId, userId));
        return ResponseEntity.noContent().build();
    }
}
