package com.nishant.coursemanagement.service.enrollment;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import com.nishant.coursemanagement.entity.Enrollment;
import com.nishant.coursemanagement.mapper.EnrollmentMapper;
import com.nishant.coursemanagement.mapper.PageMapper;
import com.nishant.coursemanagement.repository.enrollment.EnrollmentRepository;
import com.nishant.coursemanagement.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EnrollmentQueryService {

    private final EnrollmentRepository enrollmentRepository;

    public PageResponse<EnrollmentResponse> findEnrollments(Long studentId, Long courseId, Boolean active, Pageable pageable) {
        try {
            LogUtil.put("action", "QUERY_FIND_ENROLLMENTS");
            LogUtil.put("studentId", studentId);
            LogUtil.put("courseId", courseId);
            LogUtil.put("active", active);
            LogUtil.put("pageNumber", pageable.getPageNumber());
            LogUtil.put("pageSize", pageable.getPageSize());
            log.debug("Finding enrollments");
        } finally {
            LogUtil.clear();
        }
        Page<Enrollment> enrollments = enrollmentRepository.findEnrollments(studentId, courseId, active, pageable);
        return PageMapper.map(enrollments, EnrollmentMapper::toResponse);
    }

    public Enrollment findByStudentIdAndCourseId(Long studentId, Long courseId) {
        try {
            LogUtil.put("action", "QUERY_FIND_ENROLLMENT_BY_STUDENT_AND_COURSE");
            LogUtil.put("studentId", studentId);
            LogUtil.put("courseId", courseId);
            log.debug("Finding enrollment by student and course");
        } finally {
            LogUtil.clear();
        }
        Optional<Enrollment> enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        return enrollment.orElse(null);
    }
}
