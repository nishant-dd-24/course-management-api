package com.nishant.coursemanagement.dto.course;


import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Builder
public record CourseRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description
) {}
