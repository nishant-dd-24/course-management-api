package com.nishant.coursemanagement.unit.service.enrollment;

import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.Enrollment;
import com.nishant.coursemanagement.entity.Role;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.event.events.enrollment.EnrollmentChangedEvent;
import com.nishant.coursemanagement.exception.custom.CustomBadRequestException;
import com.nishant.coursemanagement.exception.custom.DuplicateResourceException;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.repository.enrollment.EnrollmentRepository;
import com.nishant.coursemanagement.service.enrollment.EnrollmentQueryService;
import com.nishant.coursemanagement.service.enrollment.EnrollmentServiceImpl;
import com.nishant.coursemanagement.unit.service.BaseUnitTest;
import com.nishant.coursemanagement.service.course.CourseQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnrollmentUnitTests extends BaseUnitTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseQueryService courseQueryService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentQueryService enrollmentQueryService;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private static final Long COURSE_ID = 1L;
    private static final Long STUDENT_ID = 2L;
    private static final Long INSTRUCTOR_ID = 3L;
    private static final Long ENROLLMENT_ID = 10L;
    private static final String COURSE_NOT_FOUND = "Course not found";
    private static final String ENROLLMENT_NOT_FOUND = "Enrollment not found";
    private static final String ALREADY_ENROLLED = "Already enrolled";
    private static final String COURSE_FULL = "Course is full";
    private static final String DB_FAILURE = "DB Failure";
    private static final String TITLE = "Course";
    private static final String DESCRIPTION = "Course Description";

    private Course buildCourse(Long id, Long enrolledStudents, Long maxSeats, User instructor) {
        return Course.builder()
                .id(id)
                .title(TITLE)
                .description(DESCRIPTION)
                .maxSeats(maxSeats)
                .enrolledStudents(enrolledStudents)
                .instructor(instructor)
                .isActive(true)
                .build();
    }

    private Enrollment buildEnrollment(Long id, User student, Course course, Boolean isActive) {
        return Enrollment.builder()
                .id(id)
                .student(student)
                .course(course)
                .isActive(isActive)
                .build();
    }

    private Enrollment captureSavedEnrollment() {
        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        return captor.getValue();
    }

    private Course captureSavedCourse() {
        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        return captor.getValue();
    }

    private void verifyNoSideEffects() {
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(courseRepository, never()).save(any(Course.class));
        verifyNoEventPublished();
    }

    @Nested
    class EnrollTests {

        @Test
        void shouldEnrollSuccessfully_whenNoExistingEnrollmentAndSeatsAvailable() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 1L, 3L, instructor);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(courseQueryService.getCourseByIdForUpdate(COURSE_ID)).thenReturn(Optional.of(course));
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(null);
            when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
                Enrollment enrollment = invocation.getArgument(0);
                enrollment.setId(ENROLLMENT_ID);
                return enrollment;
            });
            when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

            EnrollmentResponse response = enrollmentService.enroll(COURSE_ID);

            assertNotNull(response);
            assertEquals(ENROLLMENT_ID, response.id());
            assertEquals(STUDENT_ID, response.studentId());
            assertEquals(COURSE_ID, response.courseId());

            Enrollment savedEnrollment = captureSavedEnrollment();
            Course savedCourse = captureSavedCourse();

            assertEquals(STUDENT_ID, savedEnrollment.getStudent().getId());
            assertEquals(COURSE_ID, savedEnrollment.getCourse().getId());
            assertTrue(savedEnrollment.getIsActive());
            assertEquals(2L, savedCourse.getEnrolledStudents());

            EnrollmentChangedEvent event = captureEvent(EnrollmentChangedEvent.class);
            assertEquals(COURSE_ID, event.courseId());
        }

        @Test
        void shouldThrowNotFound_whenCourseDoesNotExist() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(courseQueryService.getCourseByIdForUpdate(COURSE_ID)).thenReturn(Optional.empty());
            when(exceptionUtil.notFound(COURSE_NOT_FOUND)).thenReturn(new ResourceNotFoundException(COURSE_NOT_FOUND));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> enrollmentService.enroll(COURSE_ID)
            );

            assertEquals(COURSE_NOT_FOUND, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldThrowDuplicate_whenAlreadyEnrolledAndActive() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 1L, 3L, instructor);
            Enrollment existingEnrollment = buildEnrollment(ENROLLMENT_ID, student, course, true);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(courseQueryService.getCourseByIdForUpdate(COURSE_ID)).thenReturn(Optional.of(course));
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(existingEnrollment);
            when(exceptionUtil.duplicate(ALREADY_ENROLLED)).thenReturn(new DuplicateResourceException(ALREADY_ENROLLED));

            DuplicateResourceException ex = assertThrows(
                    DuplicateResourceException.class,
                    () -> enrollmentService.enroll(COURSE_ID)
            );

            assertEquals(ALREADY_ENROLLED, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldReactivateEnrollment_whenAlreadyEnrolledButInactive_andSeatsAvailable() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 1L, 3L, instructor);
            Enrollment existingEnrollment = buildEnrollment(ENROLLMENT_ID, student, course, false);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(courseQueryService.getCourseByIdForUpdate(COURSE_ID)).thenReturn(Optional.of(course));
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(existingEnrollment);
            when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

            EnrollmentResponse response = enrollmentService.enroll(COURSE_ID);

            assertNotNull(response);
            assertEquals(ENROLLMENT_ID, response.id());
            assertEquals(STUDENT_ID, response.studentId());
            assertEquals(COURSE_ID, response.courseId());
            assertTrue(existingEnrollment.getIsActive());

            Enrollment savedEnrollment = captureSavedEnrollment();
            Course savedCourse = captureSavedCourse();

            assertTrue(savedEnrollment.getIsActive());
            assertEquals(2L, savedCourse.getEnrolledStudents());

            EnrollmentChangedEvent event = captureEvent(EnrollmentChangedEvent.class);
            assertEquals(COURSE_ID, event.courseId());
        }

        @Test
        void shouldThrowBadRequest_whenReactivatingButCourseIsFull() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 3L, 3L, instructor);
            Enrollment existingEnrollment = buildEnrollment(ENROLLMENT_ID, student, course, false);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(courseQueryService.getCourseByIdForUpdate(COURSE_ID)).thenReturn(Optional.of(course));
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(existingEnrollment);
            when(exceptionUtil.badRequest(COURSE_FULL)).thenReturn(new CustomBadRequestException(COURSE_FULL));

            CustomBadRequestException ex = assertThrows(
                    CustomBadRequestException.class,
                    () -> enrollmentService.enroll(COURSE_ID)
            );

            assertEquals(COURSE_FULL, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldThrowBadRequest_whenCourseIsFullAndNoExistingEnrollment() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 3L, 3L, instructor);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(courseQueryService.getCourseByIdForUpdate(COURSE_ID)).thenReturn(Optional.of(course));
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(null);
            when(exceptionUtil.badRequest(COURSE_FULL)).thenReturn(new CustomBadRequestException(COURSE_FULL));

            CustomBadRequestException ex = assertThrows(
                    CustomBadRequestException.class,
                    () -> enrollmentService.enroll(COURSE_ID)
            );

            assertEquals(COURSE_FULL, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldThrowDuplicate_whenDataIntegrityViolationOccurs() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 1L, 3L, instructor);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(courseQueryService.getCourseByIdForUpdate(COURSE_ID)).thenReturn(Optional.of(course));
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(null);
            when(enrollmentRepository.save(any(Enrollment.class))).thenThrow(new DataIntegrityViolationException(DB_FAILURE));
            when(exceptionUtil.duplicate(ALREADY_ENROLLED)).thenReturn(new DuplicateResourceException(ALREADY_ENROLLED));

            DuplicateResourceException ex = assertThrows(
                    DuplicateResourceException.class,
                    () -> enrollmentService.enroll(COURSE_ID)
            );

            assertEquals(ALREADY_ENROLLED, ex.getMessage());
            verify(enrollmentRepository).save(any(Enrollment.class));
            verify(courseRepository, never()).save(any(Course.class));
            verify(eventPublisher, never()).publishEvent(any());
            verify(courseQueryService).getCourseByIdForUpdate(COURSE_ID);
        }
    }

    @Nested
    class UnenrollTests {

        @Test
        void shouldUnenrollSuccessfully_whenEnrollmentExistsAndActive() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 2L, 4L, instructor);
            Enrollment enrollment = buildEnrollment(ENROLLMENT_ID, student, course, true);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(enrollment);
            when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

            enrollmentService.unenroll(COURSE_ID);

            assertFalse(enrollment.getIsActive());

            Enrollment savedEnrollment = captureSavedEnrollment();
            Course savedCourse = captureSavedCourse();

            assertFalse(savedEnrollment.getIsActive());
            assertEquals(1L, savedCourse.getEnrolledStudents());

            EnrollmentChangedEvent event = captureEvent(EnrollmentChangedEvent.class);
            assertEquals(COURSE_ID, event.courseId());
        }

        @Test
        void shouldThrowNotFound_whenEnrollmentDoesNotExist() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(null);
            when(exceptionUtil.notFound(ENROLLMENT_NOT_FOUND)).thenReturn(new ResourceNotFoundException(ENROLLMENT_NOT_FOUND));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> enrollmentService.unenroll(COURSE_ID)
            );

            assertEquals(ENROLLMENT_NOT_FOUND, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldThrowNotFound_whenEnrollmentIsInactive() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 2L, 4L, instructor);
            Enrollment enrollment = buildEnrollment(ENROLLMENT_ID, student, course, false);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(enrollment);
            when(exceptionUtil.notFound(ENROLLMENT_NOT_FOUND)).thenReturn(new ResourceNotFoundException(ENROLLMENT_NOT_FOUND));

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> enrollmentService.unenroll(COURSE_ID)
            );

            assertEquals(ENROLLMENT_NOT_FOUND, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldNotDecrementBelowZero_whenCourseHasZeroStudents() {
            User student = buildUser(STUDENT_ID, Role.STUDENT);
            User instructor = buildUser(INSTRUCTOR_ID, Role.INSTRUCTOR);
            Course course = buildCourse(COURSE_ID, 0L, 4L, instructor);
            Enrollment enrollment = buildEnrollment(ENROLLMENT_ID, student, course, true);

            when(authUtil.getCurrentUser()).thenReturn(student);
            when(enrollmentQueryService.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(enrollment);
            when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

            enrollmentService.unenroll(COURSE_ID);

            Enrollment savedEnrollment = captureSavedEnrollment();
            Course savedCourse = captureSavedCourse();

            assertFalse(savedEnrollment.getIsActive());
            assertEquals(0L, savedCourse.getEnrolledStudents());

            EnrollmentChangedEvent event = captureEvent(EnrollmentChangedEvent.class);
            assertEquals(COURSE_ID, event.courseId());
        }
    }

}
