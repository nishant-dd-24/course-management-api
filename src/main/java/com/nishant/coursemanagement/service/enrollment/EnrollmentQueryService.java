package com.nishant.coursemanagement.service.enrollment;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import com.nishant.coursemanagement.entity.Enrollment;
import com.nishant.coursemanagement.mapper.EnrollmentMapper;
import com.nishant.coursemanagement.mapper.PageMapper;
import com.nishant.coursemanagement.repository.enrollment.EnrollmentRepository;
import com.nishant.coursemanagement.log.annotation.Loggable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.nishant.coursemanagement.log.annotation.LogLevel.DEBUG;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EnrollmentQueryService {

    private final EnrollmentRepository enrollmentRepository;

    @Loggable(
            action = "QUERY_FIND_ENROLLMENTS",
            extras = {"#studentId", "#courseId", "#active", "#pageable.getPageNumber()", "#pageable.getPageSize()"},
            extraKeys = {"studentId", "courseId", "active", "pageNumber", "pageSize"},
            level = DEBUG
    )
    public PageResponse<EnrollmentResponse> findEnrollments(Long studentId, Long courseId, Boolean active, Pageable pageable) {
        Page<Enrollment> enrollments = enrollmentRepository.findEnrollments(studentId, courseId, active, pageable);
        return PageMapper.map(enrollments, EnrollmentMapper::toResponse);
    }

    @Loggable(
            action = "QUERY_FIND_ENROLLMENT_BY_STUDENT_AND_COURSE",
            extras = {"#studentId", "#courseId"},
            extraKeys = {"studentId", "courseId"},
            level = DEBUG
    )
    public Enrollment findByStudentIdAndCourseId(Long studentId, Long courseId) {
        Optional<Enrollment> enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        return enrollment.orElse(null);
    }
}
