package com.nishant.coursemanagement.service.enrollment;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentSearchRequest;

public interface EnrollmentService {
    EnrollmentResponse enroll(Long courseId);

    PageResponse<EnrollmentResponse> getMyEnrollments(EnrollmentSearchRequest request);

    PageResponse<EnrollmentResponse> getEnrollmentsByCourse(Long courseId, EnrollmentSearchRequest request);

    void unenroll(Long courseId);
}
