package com.nishant.coursemanagement.dto.course;

import lombok.Builder;

@Builder
public record CourseResponse (Long id, String title, String description, Long instructorId, Long maxSeats) {}
