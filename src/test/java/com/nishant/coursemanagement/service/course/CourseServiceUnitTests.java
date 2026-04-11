package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.event.events.course.CourseUpdatedEvent;
import com.nishant.coursemanagement.exception.custom.CustomBadRequestException;
import com.nishant.coursemanagement.exception.custom.ResourceNotFoundException;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.service.BaseServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceUnitTests extends BaseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseQueryService courseQueryService;

    @InjectMocks
    private CourseServiceImpl courseService;

    private static final String OLD_TITLE = "Title";
    private static final String OLD_DESCRIPTION = "Description";
    private static final String NEW_RAW_TITLE = "   New Title  ";
    private static final String NEW_RAW_DESCRIPTION = "   Request Description  ";
    private static final String NEW_SANITIZED_TITLE = "New Title";
    private static final String NEW_SANITIZED_DESCRIPTION = "Request Description";
    private static final String NOT_FOUND = "Course not found";
    private static final String DB_FAILURE = "DB Failure";
    private static final String TITLE_BLANK = "Title cannot be blank";
    private static final String DESCRIPTION_BLANK = "Description cannot be blank";
    private static final String PATCH_EMPTY = "At least one field must be provided for patching";


    private Course buildTestCourse(User user, Long courseId){
        return Course.builder()
                .id(courseId)
                .enrolledStudents(10L)
                .maxSeats(15L)
                .description(OLD_DESCRIPTION)
                .title(OLD_TITLE)
                .instructor(user)
                .isActive(true)
                .build();
    }

    private CourseRequest buildTestCourseRequest(){
        return CourseRequest.builder()
                .title(NEW_RAW_TITLE)
                .description(NEW_RAW_DESCRIPTION)
                .maxSeats(20L)
                .build();
    }

    private User instructor;
    private User currentUser;
    private Course course;
    private Long courseId;

    @BeforeEach
    void setUp() {
        courseId = 1L;
        instructor = buildUser(1L);
        currentUser = buildUser(1L);
        course = buildTestCourse(instructor, courseId);
    }

    private void mockValidUserAndCourse() {
        mockCurrentUser(currentUser);
        when(courseQueryService.getCourseById(courseId)).thenReturn(course);
    }

    private void verifyNoSideEffects(){
        verify(courseRepository, never()).save(any());
        verifyNoEventPublished();
    }

    @Nested
    class CreateCourseTests{
        @Test
        void shouldCreateCourseSuccessfully() {
            CourseRequest request = buildTestCourseRequest();
            when(authUtil.getCurrentUser()).thenReturn(instructor);
            course.setTitle(NEW_SANITIZED_TITLE);
            ArgumentCaptor<Course> courseArgumentCaptor = ArgumentCaptor.forClass(Course.class);
            when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> course);
            CourseResponse response = courseService.createCourse(request);
            assertNotNull(response);
            assertEquals(NEW_SANITIZED_TITLE, response.title());
            verify(authUtil).getCurrentUser();
            verify(courseRepository).save(courseArgumentCaptor.capture());
            Course savedCourse = courseArgumentCaptor.getValue();
            assertEquals(NEW_SANITIZED_TITLE, savedCourse.getTitle());
            assertEquals(instructor, savedCourse.getInstructor());
            CourseUpdatedEvent event = captureEvent(CourseUpdatedEvent.class);
            assertEquals(courseId, event.courseId());
        }

        @Test
        void shouldThrowException_whenRepositoryFails(){
            CourseRequest request = buildTestCourseRequest();
            when(authUtil.getCurrentUser()).thenReturn(instructor);
            when(courseRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));
            assertThrows(RuntimeException.class, () -> courseService.createCourse(request));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class UpdateCourseTests{
        @Test
        void shouldUpdateCourseSuccessfully() {
            CourseRequest request = buildTestCourseRequest();
            mockValidUserAndCourse();
            when(courseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            CourseResponse response = courseService.updateCourse(courseId, request);
            assertNotNull(response);
            assertEquals(NEW_SANITIZED_TITLE, response.title());
            assertEquals(NEW_SANITIZED_TITLE, course.getTitle());
            assertEquals(NEW_SANITIZED_DESCRIPTION, course.getDescription());
            assertEquals(20L, course.getMaxSeats());
            verify(courseRepository).save(course);
            CourseUpdatedEvent event = captureEvent(CourseUpdatedEvent.class);
            assertEquals(courseId, event.courseId());
        }

        @Test
        void shouldThrowException_whenUserIsNotOwner() {
            currentUser.setId(2L);
            mockValidUserAndCourse();
            mockNotFoundException(NOT_FOUND);
            ResourceNotFoundException ex =  assertThrows(ResourceNotFoundException.class, () -> courseService.updateCourse(courseId, buildTestCourseRequest()));
            assertEquals(NOT_FOUND, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldThrowException_whenCourseNotFound() {
            when(authUtil.getCurrentUser()).thenReturn(currentUser);
            when(courseQueryService.getCourseById(courseId))
                    .thenThrow(new ResourceNotFoundException(NOT_FOUND));
            assertThrows(ResourceNotFoundException.class, () -> courseService.updateCourse(courseId, buildTestCourseRequest()));
            verifyNoSideEffects();
        }

        @Test
        void shouldThrowException_whenRepositoryFails() {
            mockValidUserAndCourse();
            when(courseRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));
            assertThrows(RuntimeException.class, () -> courseService.updateCourse(courseId, buildTestCourseRequest()));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class PatchCourseTests{

        private CoursePatchRequest buildTestCoursePatchRequest(Boolean isTitleNull, Boolean isDescNull, Boolean isMaxSeatsNull){
            String title = (isTitleNull) ? null : NEW_RAW_TITLE;
            String description = (isDescNull) ? null : NEW_RAW_DESCRIPTION;
            Long maxSeats = (isMaxSeatsNull) ? null : 20L;
            return new CoursePatchRequest(title, description, maxSeats);
        }

        @Test
        void shouldPatchCourseSuccessfully() {
            CoursePatchRequest request = buildTestCoursePatchRequest(false, true, true);
            mockValidUserAndCourse();
            when(courseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            CourseResponse response = courseService.patchCourse(courseId, request);
            assertNotNull(response);
            assertEquals(NEW_SANITIZED_TITLE, course.getTitle()); // normalized
            assertEquals(OLD_DESCRIPTION, course.getDescription()); // unchanged
            assertEquals(15L, course.getMaxSeats()); // unchanged
            verify(courseRepository).save(course);
            CourseUpdatedEvent event = captureEvent(CourseUpdatedEvent.class);
            assertEquals(courseId, event.courseId());
        }

        @Test
        void shouldThrowException_whenPatchRequestIsEmpty() {
            CoursePatchRequest request = buildTestCoursePatchRequest(true, true, true);
            mockBadRequestException(PATCH_EMPTY);
            assertThrows(CustomBadRequestException.class, () -> courseService.patchCourse(1L, request));
            verify(courseRepository, never()).save(any());
        }

        @Test
        void shouldThrowException_whenTitleIsBlank() {
            CoursePatchRequest request = new CoursePatchRequest("   ", null, null);
            mockBadRequestException(TITLE_BLANK);
            assertThrows(CustomBadRequestException.class, () -> courseService.patchCourse(1L, request));
        }

        @Test
        void shouldThrowException_whenDescriptionIsBlank() {
            CoursePatchRequest request = new CoursePatchRequest(null, "   ", null);
            mockBadRequestException(DESCRIPTION_BLANK);
            assertThrows(CustomBadRequestException.class, () -> courseService.patchCourse(1L, request));
        }

        @Test
        void shouldThrowException_whenUserIsNotOwner() {
            currentUser.setId(2L);
            mockValidUserAndCourse();
            mockNotFoundException(NOT_FOUND);
            CoursePatchRequest request = buildTestCoursePatchRequest(false, true, true);
            ResourceNotFoundException ex =  assertThrows(ResourceNotFoundException.class, () -> courseService.patchCourse(courseId, request));
            assertEquals(NOT_FOUND, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldThrowException_whenRepositoryFails() {
            mockValidUserAndCourse();
            when(courseRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));
            CoursePatchRequest request = buildTestCoursePatchRequest(true, false, false);
            assertThrows(RuntimeException.class, () -> courseService.patchCourse(courseId, request));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    class DeactivateCourseTests{
        @Test
        void shouldDeactivateCourseSuccessfully() {
            mockValidUserAndCourse();
            when(courseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            courseService.deactivateCourse(courseId);
            assertFalse(course.getIsActive());
            verify(courseRepository).save(argThat(c -> !c.getIsActive()));
            CourseUpdatedEvent event = captureEvent(CourseUpdatedEvent.class);
            assertEquals(courseId, event.courseId());
        }

        @Test
        void shouldThrowException_whenUserIsNotOwner() {
            currentUser.setId(2L);
            mockValidUserAndCourse();
            mockNotFoundException(NOT_FOUND);
            ResourceNotFoundException ex =  assertThrows(ResourceNotFoundException.class, () -> courseService.deactivateCourse(courseId));
            assertEquals(NOT_FOUND, ex.getMessage());
            verifyNoSideEffects();
        }

        @Test
        void shouldThrowException_whenRepositoryFails() {
            mockValidUserAndCourse();
            when(courseRepository.save(any())).thenThrow(new RuntimeException(DB_FAILURE));
            assertThrows(RuntimeException.class, () -> courseService.deactivateCourse(courseId));
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

}
