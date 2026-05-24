package com.liveclass.course.controller.waitlist;

import com.liveclass.course.controller.waitlist.response.MyWaitlistItemResponse;
import com.liveclass.course.controller.waitlist.response.WaitlistResponse;
import com.liveclass.course.domain.waitlistentry.WaitlistEntry;
import com.liveclass.course.global.dto.request.PageOptions;
import com.liveclass.course.global.dto.response.PageResponse;
import com.liveclass.course.service.ports.in.WaitlistService;
import com.liveclass.course.service.ports.in.command.waitlist.CancelWaitlistCommand;
import com.liveclass.course.service.ports.in.command.waitlist.RegisterWaitlistCommand;
import com.liveclass.course.service.ports.in.result.waitlist.MyWaitlistListItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "[회원]대기열", description = "강의 대기열 등록/이탈 API")
public class WaitlistController {

    private final WaitlistService waitlistService;

    @GetMapping("/users/{userId}/waitlist")
    @Operation(
        summary = "내 대기열 목록 조회",
        description = "사용자가 등록한 대기열 항목을 시간순(createdAt ASC)으로 조회합니다. 강의 정보 포함."
    )
    public ResponseEntity<PageResponse<MyWaitlistItemResponse>> searchMyWaitlist(
        @PathVariable Long userId,
        @ModelAttribute PageOptions options
    ) {
      Page<MyWaitlistListItem> page = waitlistService.searchMyWaitlist(userId, options.pageable());
      return ResponseEntity.ok(PageResponse.of(page, MyWaitlistItemResponse::from));
    }

    @PostMapping("/classes/{classId}/waitlist")
    @Operation(
            summary = "대기열 등록",
            description = "정원이 찬 OPEN 강의에 대기열 등록. FIFO 순번 자동 할당. 비관적 락으로 position 충돌 방지."
    )
    public ResponseEntity<WaitlistResponse> register(
            @PathVariable Long classId,
            @RequestParam @NotNull Long userId
    ) {
        WaitlistEntry entry = waitlistService.register(new RegisterWaitlistCommand(classId, userId));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(WaitlistResponse.from(entry));
    }

    @DeleteMapping("/waitlist/{waitlistId}")
    @Operation(
            summary = "대기열 이탈",
            description = "본인 대기열 항목을 soft delete. 뒷 순번 position은 불변 (gap 허용)."
    )
    public ResponseEntity<Void> cancel(
            @PathVariable Long waitlistId,
            @RequestParam @NotNull Long userId
    ) {
        waitlistService.cancel(new CancelWaitlistCommand(waitlistId, userId));
        return ResponseEntity.noContent().build();
    }
}
