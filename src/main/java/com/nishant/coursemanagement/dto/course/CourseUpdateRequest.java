package com.nishant.coursemanagement.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "Course full-update request payload")
public record CourseUpdateRequest(
        @Schema(description = "Updated course title", example = "Java Backend Mastery")
        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "Updated course description", example = "Build production-grade APIs with Spring Boot")
        @NotBlank(message = "Description is required")
        String description,

        @Schema(description = "Updated maximum seats", example = "30")
        @Min(value = 1, message = "Max seats must be at least 1, or you can leave it null for keeping max seats unchanged")
        Long maxSeats
) {
}
