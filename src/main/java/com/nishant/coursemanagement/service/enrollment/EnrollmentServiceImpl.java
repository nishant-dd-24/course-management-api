package com.nishant.coursemanagement.service.enrollment;

import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.Enrollment;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.EnrollmentMapper;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.repository.enrollment.EnrollmentRepository;
import com.nishant.coursemanagement.security.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService{

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final AuthUtil authUtil;
    private final ExceptionUtil exceptionUtil;

    private void validateCourseOwnership(Course course, User currentUser) {
        if(!course.getInstructor().getId().equals(currentUser.getId())) {
            throw exceptionUtil.notFound("Course not found");
        }
    }

    @Override
    public EnrollmentResponse enroll(Long courseId){
        User currentUser = authUtil.getCurrentUser();
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found"));
        log.info("action=ENROLL userId={} courseId={}", currentUser.getId(), courseId);

        Enrollment existingEnrollment = enrollmentRepository.findByStudentIdAndCourseId(currentUser.getId(), courseId)
                .orElse(null);
        if(existingEnrollment != null) {
            log.warn("action=ENROLL_DUPLICATE userId={} courseId={}", currentUser.getId(), courseId);
            if(existingEnrollment.getIsActive()){
                log.warn("action=ENROLL_FAILED reason=ALREADY_ENROLLED_ACTIVE userId={} courseId={}", currentUser.getId(), courseId);
                throw exceptionUtil.duplicate("Already enrolled");
            }
            else {
                log.info("action=REACTIVATE_ENROLLMENT userId={} courseId={}", currentUser.getId(), courseId);
                if(course.getEnrolledStudents() >= course.getMaxSeats()) {
                    log.warn("action=ENROLL_FAILED reason=COURSE_FULL userId={} courseId={}", currentUser.getId(), courseId);
                    throw exceptionUtil.badRequest("Course is full");
                }
                log.info("action=REACTIVATE_ENROLLMENT_SUCCESS userId={} courseId={}", currentUser.getId(), courseId);
                existingEnrollment.setIsActive(true);
                course.setEnrolledStudents(course.getEnrolledStudents() + 1);
                courseRepository.save(course);
                return EnrollmentMapper.toResponse(enrollmentRepository.save(existingEnrollment));
            }
        }

        if(course.getEnrolledStudents() >= course.getMaxSeats()) {
            log.warn("action=ENROLL_FAILED reason=COURSE_FULL userId={} courseId={}", currentUser.getId(), courseId);
            throw exceptionUtil.badRequest("Course is full");
        }
        course.setEnrolledStudents(course.getEnrolledStudents() + 1);
        Enrollment enrollment = Enrollment.builder()
                .student(currentUser)
                .course(course)
                .build();

        log.info("action=ENROLL_SUCCESS userId={} courseId={}", currentUser.getId(), courseId);
        try{
            enrollment = enrollmentRepository.save(enrollment);
            courseRepository.save(course);
        }
        catch (DataIntegrityViolationException e){
            log.warn("action=ENROLL_FAILED reason=DATA_INTEGRITY_VIOLATION userId={} courseId={} error={}", currentUser.getId(), courseId, e.getMessage());
            throw exceptionUtil.duplicate("Already enrolled");
        }
        return EnrollmentMapper.toResponse(enrollment);
    }

    @Override
    public Page<EnrollmentResponse> getMyEnrollments(Boolean active, Pageable pageable){
        User currentUser = authUtil.getCurrentUser();
        log.info("action=GET_MY_ENROLLMENTS userId={} active={}", currentUser.getId(), active);

        return enrollmentRepository.findEnrollments(currentUser.getId(), null, active, pageable)
                .map(EnrollmentMapper::toResponse);
    }

    @Override
    public Page<EnrollmentResponse> getEnrollmentsByCourse(Long courseId, Boolean active, Pageable pageable){
        User currentUser = authUtil.getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found"));
        validateCourseOwnership(course, currentUser);
        log.info("action=GET_ENROLLMENTS_BY_COURSE userId={} courseId={} active={}", currentUser.getId(), courseId, active);
        return enrollmentRepository.findEnrollments(null, courseId, active, pageable)
                .map(EnrollmentMapper::toResponse);
    }

    @Override
    public void unenroll(Long courseId){
        User currentUser = authUtil.getCurrentUser();
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(currentUser.getId(), courseId)
                .orElseThrow(() -> exceptionUtil.notFound("Enrollment not found"));
        log.warn("action=UNENROLL userId={} courseId={}", currentUser.getId(), courseId);
        enrollment.setIsActive(false);
        enrollmentRepository.save(enrollment);
        Course course = enrollment.getCourse();
        if(course.getEnrolledStudents()>0) course.setEnrolledStudents(course.getEnrolledStudents() - 1);
        courseRepository.save(course);
    }
}
