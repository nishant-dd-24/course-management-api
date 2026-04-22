package com.nishant.coursemanagement.dto.course;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;


@Builder
@Schema(description = "Course creation request payload")
public record CourseRequest(
        @Schema(description = "Course title", example = "Java Backend Mastery")
        @NotBlank(message = "Title is required")
        @Size(max = 100, message = "Title must be at most 100 characters")
        String title,

        @Schema(description = "Course description", example = "Build production-grade APIs with Spring Boot")
        @NotBlank(message = "Description is required")
        String description,

        @Schema(description = "Maximum seats (defaults to 20 when omitted)", example = "30")
        @Min(value = 1, message = "Max seats must be at least 1, or you can leave it null for default value of 20")
        Long maxSeats
) {
}
