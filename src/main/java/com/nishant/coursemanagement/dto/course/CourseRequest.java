package com.nishant.coursemanagement.dto.course;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;


@Builder
public record CourseRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @Min(value = 1, message = "Max seats must be at least 1, or you can leave it null for default value of 20")
        Long maxSeats
) {
}
