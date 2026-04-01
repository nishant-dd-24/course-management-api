package com.nishant.coursemanagement.service.enrollment;

import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EnrollmentService {
    EnrollmentResponse enroll(Long courseId);
    Page<EnrollmentResponse> getMyEnrollments(Boolean active, Pageable pageable);
    Page<EnrollmentResponse> getEnrollmentsByCourse(Long courseId, Boolean active, Pageable pageable);
    void unenroll(Long courseId);
}
