package com.nishant.coursemanagement.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
@Schema(description = "Course partial-update request payload")
public record CoursePatchRequest(
        @Schema(description = "Updated course title (optional)", example = "Java Backend Mastery")
        String title,
        @Schema(description = "Updated course description (optional)", example = "Build production-grade APIs with Spring Boot")
        String description,
        @Schema(description = "Updated maximum seats (optional)", example = "30")
        @Min(value = 1, message = "Max seats must be at least 1")
        Long maxSeats
) {
}
