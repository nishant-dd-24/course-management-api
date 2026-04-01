package com.nishant.coursemanagement.dto.enrollment;

import lombok.Builder;

@Builder
public record EnrollmentResponse(Long id, Long studentId, Long courseId) {}
