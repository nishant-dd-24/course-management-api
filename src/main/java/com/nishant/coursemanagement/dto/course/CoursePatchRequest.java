package com.nishant.coursemanagement.dto.course;

import lombok.Builder;

@Builder
public record CoursePatchRequest(
        String title,
        String description
) {}
