package com.liveclass.course.controller.liveclass;

import com.liveclass.course.controller.liveclass.request.CreateClassRequest;
import com.liveclass.course.controller.liveclass.request.SearchClassOptions;
import com.liveclass.course.controller.liveclass.response.ClassListItemResponse;
import com.liveclass.course.controller.liveclass.response.ClassResponse;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.global.dto.response.PageResponse;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.service.ports.in.result.liveclass.ClassDetail;
import com.liveclass.course.service.ports.in.result.liveclass.ClassListItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Validated
@Tag(name="강의",description = "강의 관련 API")

public class LiveClassController {

    private final LiveClassService liveClassService;

    @PostMapping
    @Operation(summary = "강의 등록", description = "제목, 설명, 가격, 정원(최대 수강 인원), 수강 기간(시작일~종료일)을 입력해 강의를 등록합니다.")
    public ResponseEntity<ClassResponse> create(
            @RequestParam @NotNull Long creatorId,
            @Valid @RequestBody CreateClassRequest request
    ) {
        LiveClass liveClass = liveClassService.create(request.toCommand(creatorId));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ClassResponse.from(liveClass, 0L));
    }

    @GetMapping("/{classId}")
    @Operation(summary = "강의 상세 조회", description = "강의에 대한 상세조회 입니다(현재 신청 인원 포함).")
    public ResponseEntity<ClassResponse> get(@PathVariable Long classId) {
        ClassDetail detail = liveClassService.get(classId);
        return ResponseEntity.ok(
                ClassResponse.from(detail.liveClass(), detail.currentEnrolled())
        );
    }

    @GetMapping
    @Operation(summary = "강의 목록 조회", description = "강의 목록 조회이며 상태로 필터 가능합니다.")
    public ResponseEntity<PageResponse<ClassListItemResponse>> search(
        @ModelAttribute SearchClassOptions options
    ) {
        Page<ClassListItem> page = liveClassService.search(options.toCommand(), options.pageable());
        return ResponseEntity.ok(PageResponse.of(page, ClassListItemResponse::from));
    }
}
