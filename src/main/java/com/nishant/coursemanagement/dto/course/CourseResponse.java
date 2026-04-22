package com.nishant.coursemanagement.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Course response payload")
public record CourseResponse(
        @Schema(description = "Course ID", example = "10") Long id,
        @Schema(description = "Course title", example = "Java Backend Mastery") String title,
        @Schema(description = "Course description", example = "Build production-grade APIs with Spring Boot") String description,
        @Schema(description = "Instructor user ID", example = "2") Long instructorId,
        @Schema(description = "Maximum seats", example = "30") Long maxSeats,
        @Schema(description = "Available seats", example = "24") Long availableSeats,
        @Schema(description = "Whether course is active", example = "true") Boolean isActive
) {
}
