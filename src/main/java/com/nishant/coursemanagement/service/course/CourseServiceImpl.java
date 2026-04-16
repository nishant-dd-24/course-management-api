package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.dto.course.CourseUpdateRequest;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.event.events.course.CourseUpdatedEvent;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.CourseMapper;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.security.AuthUtil;
import com.nishant.coursemanagement.log.annotation.Loggable;
import com.nishant.coursemanagement.util.Sanitizer;
import com.nishant.coursemanagement.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.nishant.coursemanagement.log.annotation.LogLevel.DEBUG;
import static com.nishant.coursemanagement.log.annotation.LogLevel.WARN;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final AuthUtil authUtil;
    private final ExceptionUtil exceptionUtil;
    private final CourseQueryService courseQueryService;
    private final ApplicationEventPublisher eventPublisher;

    private void validateCourseOwnership(Course course, User currentUser) {
        if (!course.getInstructor().getId().equals(currentUser.getId())) {
            throw exceptionUtil.notFound("Course not found");
        }
    }

    @Override
    @Loggable(
            action = "CREATE_COURSE",
            includeCurrentUser = true
    )
    public CourseResponse createCourse(CourseRequest request) {
        User currentUser = authUtil.getCurrentUser();
        request = Sanitizer.sanitizeCourseRequest(request);
        Course course = CourseMapper.toEntity(request, currentUser);
        Course saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseUpdatedEvent(saved.getId()));
        return CourseMapper.toResponse(saved);
    }

    @Override
    @Loggable(
            action = "GET_COURSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public CourseResponse getCourseById(Long id) {
        return courseQueryService.getCourseResponseById(id);
    }

    @Override
    @Loggable(
            action = "GET_ACTIVE_COURSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public CourseResponse getActiveCourse(Long id) {
        return courseQueryService.getActiveCourseResponse(id);
    }

    @Override
    @Loggable(
            action = "GET_ALL_COURSES",
            extras = {"#title", "#isActive", "#instructorId", "#pageable.getPageNumber()", "#pageable.getPageSize()"},
            extraKeys = {"title", "isActive", "instructorId", "pageNumber", "pageSize"},
            level = DEBUG
    )
    public PageResponse<CourseResponse> getAllCourses(String title, Boolean active, Long instructorId, Pageable pageable) {
        title = StringUtil.makeQueryLike(title);
        return courseQueryService.getAllCourses(title, active, instructorId, pageable);
    }

    @Override
    @Loggable(
            action = "GET_ALL_ACTIVE_COURSES",
            extras = {"#title", "#instructorId", "#pageable.getPageNumber()", "#pageable.getPageSize()"},
            extraKeys = {"title", "instructorId", "pageNumber", "pageSize"},
            level = DEBUG
    )
    public PageResponse<CourseResponse> getAllActiveCourses(String title, Long instructorId, Pageable pageable) {
        title = StringUtil.makeQueryLike(title);
        return courseQueryService.getAllCourses(title, true, instructorId, pageable);
    }

    @Override
    @Loggable(
            action = "GET_MY_COURSES",
            extras = {"#title", "#isActive", "#pageable.getPageNumber()", "#pageable.getPageSize()"},
            extraKeys = {"title", "isActive", "pageNumber", "pageSize"},
            includeCurrentUser = true,
            level = DEBUG
    )
    public PageResponse<CourseResponse> getMyCourses(String title, Boolean active, Pageable pageable) {
        title = StringUtil.makeQueryLike(title);
        return courseQueryService.getAllCourses(title, active, authUtil.getCurrentUser().getId(), pageable);
    }

    @Override
    @Loggable(
            action = "UPDATE_COURSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            includeCurrentUser = true
    )
    public CourseResponse updateCourse(Long id, CourseUpdateRequest request) {
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getCourseById(id);
        validateCourseOwnership(course, currentUser);
        request = Sanitizer.sanitizeCourseUpdateRequest(request);
        course.setTitle(request.title());
        course.setDescription(request.description());
        if(request.maxSeats()!=null)course.setMaxSeats(request.maxSeats());
        Course saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseUpdatedEvent(saved.getId()));
        return CourseMapper.toResponse(saved);
    }

    @Loggable(
            action = "PATCH_COURSE_PAYLOAD",
            level = DEBUG,
            message = "Applying patch to course",
            extras = {"#course.getId()", "#request.title()", "#request.description()", "#request.maxSeats()"},
            extraKeys = {"courseId", "title", "description", "maxSeats"}
    )
    private void applyPatch(Course course, CoursePatchRequest request) {
        Optional.ofNullable(request.title())
                .map(StringUtil::normalize)
                .ifPresent(course::setTitle);
        Optional.ofNullable(request.description())
                .map(StringUtil::normalize)
                .ifPresent(course::setDescription);
        Optional.ofNullable(request.maxSeats())
                .ifPresent(course::setMaxSeats);
    }

    private void validatePatchRequest(CoursePatchRequest request) {
        if (request.title() == null && request.description() == null && request.maxSeats() == null) {
            throw exceptionUtil.badRequest("At least one field must be provided for patching");
        }
        if (StringUtil.isBlankButNotNull(request.title())) {
            throw exceptionUtil.badRequest("Title cannot be blank");
        }
        if (StringUtil.isBlankButNotNull(request.description())) {
            throw exceptionUtil.badRequest("Description cannot be blank");
        }
    }

    @Override
    @Loggable(
            action = "PATCH_COURSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            includeCurrentUser = true
    )
    public CourseResponse patchCourse(Long id, CoursePatchRequest request) {
        validatePatchRequest(request);
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getCourseById(id);
        validateCourseOwnership(course, currentUser);
        applyPatch(course, request);
        Course saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseUpdatedEvent(saved.getId()));
        return CourseMapper.toResponse(saved);
    }

    @Override
    @Loggable(
            action = "DEACTIVATE_COURSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            includeCurrentUser = true,
            level = WARN
    )
    public void deactivateCourse(Long id) {
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getCourseById(id);
        validateCourseOwnership(course, currentUser);
        course.setIsActive(false);
        Course saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseUpdatedEvent(saved.getId()));
    }
}
