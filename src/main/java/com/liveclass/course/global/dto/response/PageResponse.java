package com.liveclass.course.global.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Schema(description = "페이지네이션 응답")
public class PageResponse<T> {

    @Schema(description = "전체 항목 수", example = "100")
    private Long total;

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    private Integer page;

    @Schema(description = "페이지 크기", example = "10")
    private Integer size;

    @Schema(description = "데이터 목록")
    private List<T> data;

    public PageResponse(Page<?> page, List<T> data) {
        this.total = page.getTotalElements();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.data = data;
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page,
                page.getContent().stream().map(mapper).toList()
        );
    }
}
