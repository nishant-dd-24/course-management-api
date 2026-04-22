package com.nishant.coursemanagement.dto.enrollment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Enrollment response payload")
public record EnrollmentResponse(
		@Schema(description = "Enrollment ID", example = "100") Long id,
		@Schema(description = "Student user ID", example = "1") Long studentId,
		@Schema(description = "Course ID", example = "10") Long courseId
) {
}
