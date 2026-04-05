package com.nishant.coursemanagement.service.enrollment;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService {
    EnrollmentResponse enroll(Long courseId);

    PageResponse<EnrollmentResponse> getMyEnrollments(Boolean active, Pageable pageable);

    PageResponse<EnrollmentResponse> getEnrollmentsByCourse(Long courseId, Boolean active, Pageable pageable);

    void unenroll(Long courseId);
}
