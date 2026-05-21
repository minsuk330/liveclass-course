package com.liveclass.course.global.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ParameterObject
@Schema(description = "페이지네이션 옵션")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PageOptions {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
    private Integer page;

    @Schema(description = "페이지 크기 (최대 100)", example = "10", defaultValue = "10")
    private Integer size;

    @JsonIgnore
    public Pageable pageable() {
        int p = (page == null || page < 0) ? 0 : page;
        int sz = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, sz);
    }
}
