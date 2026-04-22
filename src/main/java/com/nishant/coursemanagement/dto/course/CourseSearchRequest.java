package com.nishant.coursemanagement.dto.course;

import com.nishant.coursemanagement.dto.common.SearchRequest;
import com.nishant.coursemanagement.dto.common.SortDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Course search and pagination query parameters")
public record CourseSearchRequest(

        @Schema(description = "Filter by course title", example = "java backend")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Schema(description = "Filter by active status", example = "true")
        Boolean isActive,

        @Schema(description = "Filter by instructor ID", example = "1")
        @Positive(message = "Instructor ID must be positive")
        Long instructorId,

        @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
        @Min(value = 0, message = "Page must be >= 0")
        Integer page,

        @Schema(description = "Page size (max 50)", example = "10", defaultValue = "5")
        @Min(value = 1, message = "Size must be >= 1")
        @Max(value = 50, message = "Size must be <= 50")
        Integer size,

        @Schema(description = "Course sort field")
        CourseSortBy sortBy,
        @Schema(description = "Sort direction", example = "ASC")
        SortDirection direction

) implements SearchRequest<CourseSortBy> {}