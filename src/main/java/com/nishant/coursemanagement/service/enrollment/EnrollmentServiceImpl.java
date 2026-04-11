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
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.log.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;
import static com.nishant.coursemanagement.log.annotation.LogLevel.ERROR;

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
    @Loggable(
            action = "ENROLL",
            extras = {"#courseId"},
            extraKeys = {"courseId"},
            includeCurrentUser = true
    )
    public EnrollmentResponse enroll(Long courseId) {
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getCourseByIdForUpdate(courseId).orElseThrow(() -> exceptionUtil.notFound("Course not found"));

        Enrollment existingEnrollment = enrollmentQueryService.findByStudentIdAndCourseId(currentUser.getId(), courseId);
        if (existingEnrollment != null) {
            if (existingEnrollment.getIsActive()) {
                LogUtil.log(log, WARN, "ENROLL_FAILED", "Already enrolled in active course", "reason", "ALREADY_ENROLLED_ACTIVE", "userId", currentUser.getId(), "courseId", courseId);
                throw exceptionUtil.duplicate("Already enrolled");
            } else {
                if (course.getEnrolledStudents() >= course.getMaxSeats()) {
                    LogUtil.log(log, WARN, "ENROLL_FAILED", "Course is full", "reason", "COURSE_FULL", "userId", currentUser.getId(), "courseId", courseId);
                    throw exceptionUtil.badRequest("Course is full");
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
            LogUtil.log(log, WARN, "ENROLL_FAILED", "Course is full", "reason", "COURSE_FULL", "userId", currentUser.getId(), "courseId", courseId);
            throw exceptionUtil.badRequest("Course is full");
        }
        Enrollment enrollment = Enrollment.builder()
                .student(currentUser)
                .course(course)
                .build();
        course.setEnrolledStudents(course.getEnrolledStudents() + 1);
        try {
            enrollment = enrollmentRepository.save(enrollment);
            Course savedCourse = courseRepository.save(course);
            eventPublisher.publishEvent(new EnrollmentChangedEvent(savedCourse.getId()));
        } catch (DataIntegrityViolationException e) {
            LogUtil.log(log, WARN, "ENROLL_FAILED", "Data integrity violation during enrollment", "reason", "DATA_INTEGRITY_VIOLATION", "userId", currentUser.getId(), "courseId", courseId, "error", e.getMessage());
            throw exceptionUtil.duplicate("Already enrolled");
        }
        return EnrollmentMapper.toResponse(enrollment);
    }

    @Override
    @Loggable(
            action = "GET_MY_ENROLLMENTS",
            extras = {"#active"},
            extraKeys = {"active"},
            includeCurrentUser = true
    )
    public PageResponse<EnrollmentResponse> getMyEnrollments(Boolean active, Pageable pageable) {
        User currentUser = authUtil.getCurrentUser();
        return enrollmentQueryService.findEnrollments(currentUser.getId(), null, active, pageable);
    }

    @Override
    @Loggable(
            action = "GET_ENROLLMENTS_BY_COURSE",
            extras = {"#courseId", "#active"},
            extraKeys = {"courseId", "active"},
            includeCurrentUser = true
    )
    public PageResponse<EnrollmentResponse> getEnrollmentsByCourse(Long courseId, Boolean active, Pageable pageable) {
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getActiveCourse(courseId);
        validateCourseOwnership(course, currentUser);
        return enrollmentQueryService.findEnrollments(null, courseId, active, pageable);
    }

    @Override
    @Loggable(
            action = "UNENROLL",
            extras = {"#courseId"},
            extraKeys = {"courseId"},
            includeCurrentUser = true,
            level = WARN
    )
    public void unenroll(Long courseId) {
        User currentUser = authUtil.getCurrentUser();
        Enrollment enrollment = enrollmentQueryService.findByStudentIdAndCourseId(currentUser.getId(), courseId);
        if (enrollment == null || !enrollment.getIsActive()) {
            LogUtil.log(log, WARN, "UNENROLL_FAILED", "Not enrolled in active course", "reason", "NOT_ENROLLED_ACTIVE", "userId", currentUser.getId(), "courseId", courseId);
            throw exceptionUtil.notFound("Enrollment not found");
        }
        enrollment.setIsActive(false);
        enrollmentRepository.save(enrollment);
        Course course = enrollment.getCourse();
        if (course.getEnrolledStudents() > 0) course.setEnrolledStudents(course.getEnrolledStudents() - 1);
        else {
            LogUtil.log(log, ERROR, "DATA_INCONSISTENCY", "Data inconsistency detected", "courseId", course.getId());
        }
        courseRepository.save(course);
        eventPublisher.publishEvent(new EnrollmentChangedEvent(course.getId()));
    }
}
