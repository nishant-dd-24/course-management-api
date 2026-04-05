package com.nishant.coursemanagement.service.enrollment;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.Enrollment;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.event.events.enrollment.EnrollmentChangedEvent;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.EnrollmentMapper;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.repository.enrollment.EnrollmentRepository;
import com.nishant.coursemanagement.security.AuthUtil;
import com.nishant.coursemanagement.service.course.CourseQueryService;
import com.nishant.coursemanagement.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseQueryService courseQueryService;
    private final CourseRepository courseRepository;
    private final EnrollmentQueryService enrollmentQueryService;
    private final AuthUtil authUtil;
    private final ExceptionUtil exceptionUtil;
    private final ApplicationEventPublisher eventPublisher;

    private void validateCourseOwnership(Course course, User currentUser) {
        if (!course.getInstructor().getId().equals(currentUser.getId())) {
            throw exceptionUtil.notFound("Course not found");
        }
    }

    @Override
    public EnrollmentResponse enroll(Long courseId) {
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getCourseByIdForUpdate(courseId).orElseThrow(() -> exceptionUtil.notFound("Course not found"));
        try {
            LogUtil.put("action", "ENROLL");
            LogUtil.put("userId", currentUser.getId());
            LogUtil.put("courseId", courseId);
            log.info("Enrolling in course");
        } finally {
            LogUtil.clear();
        }

        Enrollment existingEnrollment = enrollmentQueryService.findByStudentIdAndCourseId(currentUser.getId(), courseId);
        if (existingEnrollment != null) {
            try {
                LogUtil.put("action", "ENROLL_DUPLICATE");
                LogUtil.put("userId", currentUser.getId());
                LogUtil.put("courseId", courseId);
                log.warn("Duplicate enrollment attempt");
            } finally {
                LogUtil.clear();
            }
            if (existingEnrollment.getIsActive()) {
                try {
                    LogUtil.put("action", "ENROLL_FAILED");
                    LogUtil.put("reason", "ALREADY_ENROLLED_ACTIVE");
                    LogUtil.put("userId", currentUser.getId());
                    LogUtil.put("courseId", courseId);
                    log.warn("Already enrolled in active course");
                } finally {
                    LogUtil.clear();
                }
                throw exceptionUtil.duplicate("Already enrolled");
            } else {
                try {
                    LogUtil.put("action", "REACTIVATE_ENROLLMENT");
                    LogUtil.put("userId", currentUser.getId());
                    LogUtil.put("courseId", courseId);
                    log.info("Reactivating enrollment");
                } finally {
                    LogUtil.clear();
                }
                if (course.getEnrolledStudents() >= course.getMaxSeats()) {
                    try {
                        LogUtil.put("action", "ENROLL_FAILED");
                        LogUtil.put("reason", "COURSE_FULL");
                        LogUtil.put("userId", currentUser.getId());
                        LogUtil.put("courseId", courseId);
                        log.warn("Course is full");
                    } finally {
                        LogUtil.clear();
                    }
                    throw exceptionUtil.badRequest("Course is full");
                }
                try {
                    LogUtil.put("action", "REACTIVATE_ENROLLMENT_SUCCESS");
                    LogUtil.put("userId", currentUser.getId());
                    LogUtil.put("courseId", courseId);
                    log.info("Enrollment reactivated successfully");
                } finally {
                    LogUtil.clear();
                }
                existingEnrollment.setIsActive(true);
                Enrollment savedEnrollment = enrollmentRepository.save(existingEnrollment);
                course.setEnrolledStudents(course.getEnrolledStudents() + 1);
                Course savedCourse = courseRepository.save(course);
                eventPublisher.publishEvent(new EnrollmentChangedEvent(savedCourse.getId()));
                return EnrollmentMapper.toResponse(savedEnrollment);
            }
        }

        if (course.getEnrolledStudents() >= course.getMaxSeats()) {
            try {
                LogUtil.put("action", "ENROLL_FAILED");
                LogUtil.put("reason", "COURSE_FULL");
                LogUtil.put("userId", currentUser.getId());
                LogUtil.put("courseId", courseId);
                log.warn("Course is full");
            } finally {
                LogUtil.clear();
            }
            throw exceptionUtil.badRequest("Course is full");
        }
        Enrollment enrollment = Enrollment.builder()
                .student(currentUser)
                .course(course)
                .build();
        course.setEnrolledStudents(course.getEnrolledStudents() + 1);
        try {
            LogUtil.put("action", "ENROLL_SUCCESS");
            LogUtil.put("userId", currentUser.getId());
            LogUtil.put("courseId", courseId);
            log.info("Enrollment successful");
        } finally {
            LogUtil.clear();
        }
        try {
            enrollment = enrollmentRepository.save(enrollment);
            Course savedCourse = courseRepository.save(course);
            eventPublisher.publishEvent(new EnrollmentChangedEvent(savedCourse.getId()));
        } catch (DataIntegrityViolationException e) {
            try {
                LogUtil.put("action", "ENROLL_FAILED");
                LogUtil.put("reason", "DATA_INTEGRITY_VIOLATION");
                LogUtil.put("userId", currentUser.getId());
                LogUtil.put("courseId", courseId);
                LogUtil.put("error", e.getMessage());
                log.warn("Data integrity violation during enrollment");
            } finally {
                LogUtil.clear();
            }
            throw exceptionUtil.duplicate("Already enrolled");
        }
        return EnrollmentMapper.toResponse(enrollment);
    }

    @Override
    public PageResponse<EnrollmentResponse> getMyEnrollments(Boolean active, Pageable pageable) {
        User currentUser = authUtil.getCurrentUser();
        try {
            LogUtil.put("action", "GET_MY_ENROLLMENTS");
            LogUtil.put("userId", currentUser.getId());
            LogUtil.put("active", active);
            log.info("Getting my enrollments");
        } finally {
            LogUtil.clear();
        }
        return enrollmentQueryService.findEnrollments(currentUser.getId(), null, active, pageable);
    }

    @Override
    public PageResponse<EnrollmentResponse> getEnrollmentsByCourse(Long courseId, Boolean active, Pageable pageable) {
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getActiveCourse(courseId);
        validateCourseOwnership(course, currentUser);
        try {
            LogUtil.put("action", "GET_ENROLLMENTS_BY_COURSE");
            LogUtil.put("userId", currentUser.getId());
            LogUtil.put("courseId", courseId);
            LogUtil.put("active", active);
            log.info("Getting enrollments by course");
        } finally {
            LogUtil.clear();
        }
        return enrollmentQueryService.findEnrollments(null, courseId, active, pageable);
    }

    @Override
    public void unenroll(Long courseId) {
        User currentUser = authUtil.getCurrentUser();
        Enrollment enrollment = enrollmentQueryService.findByStudentIdAndCourseId(currentUser.getId(), courseId);
        if (enrollment == null || !enrollment.getIsActive()) {
            try {
                LogUtil.put("action", "UNENROLL_FAILED");
                LogUtil.put("reason", "NOT_ENROLLED_ACTIVE");
                LogUtil.put("userId", currentUser.getId());
                LogUtil.put("courseId", courseId);
                log.warn("Not enrolled in active course");
            } finally {
                LogUtil.clear();
            }
            throw exceptionUtil.notFound("Enrollment not found");
        }
        try {
            LogUtil.put("action", "UNENROLL");
            LogUtil.put("userId", currentUser.getId());
            LogUtil.put("courseId", courseId);
            log.warn("Unenrolling from course");
        } finally {
            LogUtil.clear();
        }
        enrollment.setIsActive(false);
        enrollmentRepository.save(enrollment);
        Course course = enrollment.getCourse();
        if (course.getEnrolledStudents() > 0) course.setEnrolledStudents(course.getEnrolledStudents() - 1);
        else {
            try {
                LogUtil.put("action", "DATA_INCONSISTENCY");
                LogUtil.put("courseId", course.getId());
                log.error("Data inconsistency detected");
            } finally {
                LogUtil.clear();
            }
        }
        courseRepository.save(course);
        eventPublisher.publishEvent(new EnrollmentChangedEvent(course.getId()));
    }
}
