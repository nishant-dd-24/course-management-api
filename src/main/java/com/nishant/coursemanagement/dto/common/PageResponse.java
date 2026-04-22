package com.nishant.coursemanagement.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Generic paginated response")
public record PageResponse<T>(
        @Schema(description = "Current page content")
        List<T> content,
        @Schema(description = "Current page number", example = "0")
        int pageNumber,
        @Schema(description = "Page size", example = "10")
        int pageSize,
        @Schema(description = "Total number of elements", example = "123")
        long totalElements,
        @Schema(description = "Number of elements in the current page", example = "10")
        int numberOfElements,
        @Schema(description = "Total number of pages", example = "13")
        int totalPages,
        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,
        @Schema(description = "Whether this is the last page", example = "false")
        boolean last,
        @Schema(description = "Whether a next page exists", example = "true")
        boolean hasNext,
        @Schema(description = "Whether a previous page exists", example = "false")
        boolean hasPrevious,
        @Schema(description = "Whether the page contains data", example = "true")
        boolean hasContent
) {
}
