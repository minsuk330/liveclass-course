package com.liveclass.course.controller.enrollment;

import com.liveclass.course.controller.enrollment.request.CreateEnrollmentRequest;
import com.liveclass.course.controller.enrollment.response.EnrollmentResponse;
import com.liveclass.course.domain.enrollment.Enrollment;
import com.liveclass.course.service.ports.in.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Validated
@Tag(name = "수강 신청", description = "수강 신청 관련 API")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
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
}
