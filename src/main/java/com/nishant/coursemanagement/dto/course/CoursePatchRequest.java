package com.nishant.coursemanagement.dto.course;

import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record CoursePatchRequest(
        String title,
        String description,
        @Min(value = 1, message = "Max seats must be at least 1")
        Long maxSeats
) {
}
