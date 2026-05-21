package com.liveclass.course.controller.liveclass;

import com.liveclass.course.controller.liveclass.request.CreateClassRequest;
import com.liveclass.course.controller.liveclass.response.ClassResponse;
import com.liveclass.course.domain.liveclass.LiveClass;
import com.liveclass.course.service.ports.in.LiveClassService;
import com.liveclass.course.service.ports.in.result.liveclass.ClassDetail;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
public class LiveClassController {

    private final LiveClassService liveClassService;

    @PostMapping
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
    public ResponseEntity<ClassResponse> get(@PathVariable Long classId) {
        ClassDetail detail = liveClassService.get(classId);
        return ResponseEntity.ok(
                ClassResponse.from(detail.liveClass(), detail.currentEnrolled())
        );
    }
}
