package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.entity.User;
import com.nishant.coursemanagement.event.events.course.CourseUpdatedEvent;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.CourseMapper;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.security.AuthUtil;
import com.nishant.coursemanagement.util.LogUtil;
import com.nishant.coursemanagement.util.Sanitizer;
import com.nishant.coursemanagement.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    public CourseResponse createCourse(CourseRequest request) {
        User currentUser = authUtil.getCurrentUser();
        request = Sanitizer.sanitizeCourseRequest(request);
        try {
            LogUtil.put("action", "CREATE_COURSE");
            LogUtil.put("userId", currentUser.getId());
            log.info("Creating course");
        } finally {
            LogUtil.clear();
        }
        Course course = CourseMapper.toEntity(request, currentUser);
        Course saved = courseRepository.save(course);
        try {
            LogUtil.put("action", "CREATE_COURSE_SUCCESS");
            LogUtil.put("userId", currentUser.getId());
            LogUtil.put("courseId", course.getId());
            log.info("Course created successfully");
        } finally {
            LogUtil.clear();
        }
        eventPublisher.publishEvent(new CourseUpdatedEvent(saved.getId()));
        return CourseMapper.toResponse(saved);
    }

    @Override
    public CourseResponse getCourseById(Long id) {
        try {
            LogUtil.put("action", "GET_COURSE");
            LogUtil.put("courseId", id);
            log.debug("Getting course by ID");
        } finally {
            LogUtil.clear();
        }
        return courseQueryService.getCourseResponseById(id);
    }

    @Override
    public CourseResponse getActiveCourse(Long id) {
        try {
            LogUtil.put("action", "GET_ACTIVE_COURSE");
            LogUtil.put("courseId", id);
            log.debug("Getting active course");
        } finally {
            LogUtil.clear();
        }
        return courseQueryService.getActiveCourseResponse(id);
    }

    @Override
    public PageResponse<CourseResponse> getAllCourses(String title, Boolean active, Long instructorId, Pageable pageable) {
        try {
            LogUtil.put("action", "GET_ALL_COURSES");
            LogUtil.put("title", title);
            LogUtil.put("active", active);
            LogUtil.put("instructorId", instructorId);
            LogUtil.put("pageNumber", pageable.getPageNumber());
            LogUtil.put("pageSize", pageable.getPageSize());
            log.debug("Getting all courses");
        } finally {
            LogUtil.clear();
        }
        title = StringUtil.makeQueryLike(title);
        return courseQueryService.getAllCourses(title, active, instructorId, pageable);
    }

    @Override
    public PageResponse<CourseResponse> getAllActiveCourses(String title, Long instructorId, Pageable pageable) {
        title = StringUtil.makeQueryLike(title);
        return courseQueryService.getAllCourses(title, true, instructorId, pageable);
    }

    @Override
    public PageResponse<CourseResponse> getMyCourses(String title, Boolean active, Pageable pageable) {
        try {
            LogUtil.put("action", "GET_MY_COURSES");
            LogUtil.put("title", title);
            LogUtil.put("active", active);
            LogUtil.put("pageNumber", pageable.getPageNumber());
            LogUtil.put("pageSize", pageable.getPageSize());
            log.debug("Getting my courses");
        } finally {
            LogUtil.clear();
        }
        title = StringUtil.makeQueryLike(title);
        return courseQueryService.getAllCourses(title, active, authUtil.getCurrentUser().getId(), pageable);
    }

    @Override
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getCourseById(id);
        validateCourseOwnership(course, currentUser);
        request = Sanitizer.sanitizeCourseRequest(request);
        try {
            LogUtil.put("action", "UPDATE_COURSE");
            LogUtil.put("courseId", id);
            LogUtil.put("userId", currentUser.getId());
            log.info("Updating course");
        } finally {
            LogUtil.clear();
        }
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setMaxSeats(request.maxSeats());
        try {
            LogUtil.put("action", "UPDATE_COURSE_SUCCESS");
            LogUtil.put("courseId", id);
            LogUtil.put("userId", currentUser.getId());
            log.info("Course updated successfully");
        } finally {
            LogUtil.clear();
        }
        Course saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseUpdatedEvent(saved.getId()));
        return CourseMapper.toResponse(saved);
    }

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
    public CourseResponse patchCourse(Long id, CoursePatchRequest request) {
        validatePatchRequest(request);
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getCourseById(id);
        validateCourseOwnership(course, currentUser);
        try {
            LogUtil.put("action", "PATCH_COURSE");
            LogUtil.put("courseId", id);
            LogUtil.put("userId", currentUser.getId());
            log.info("Patching course");
        } finally {
            LogUtil.clear();
        }
        try {
            LogUtil.put("action", "PATCH_COURSE_PAYLOAD");
            LogUtil.put("courseId", id);
            LogUtil.put("userId", currentUser.getId());
            LogUtil.put("request", request.toString());
            log.debug("Applying patch payload");
        } finally {
            LogUtil.clear();
        }
        applyPatch(course, request);
        Course saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseUpdatedEvent(saved.getId()));
        return CourseMapper.toResponse(saved);
    }

    @Override
    public void deactivateCourse(Long id) {
        User currentUser = authUtil.getCurrentUser();
        Course course = courseQueryService.getCourseById(id);
        validateCourseOwnership(course, currentUser);
        try {
            LogUtil.put("action", "DEACTIVATE_COURSE");
            LogUtil.put("courseId", id);
            LogUtil.put("userId", currentUser.getId());
            log.warn("Deactivating course");
        } finally {
            LogUtil.clear();
        }
        course.setIsActive(false);
        Course saved = courseRepository.save(course);
        eventPublisher.publishEvent(new CourseUpdatedEvent(saved.getId()));
    }
}
