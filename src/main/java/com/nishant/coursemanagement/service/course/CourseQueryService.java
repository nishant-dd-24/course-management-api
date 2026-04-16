package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.CourseMapper;
import com.nishant.coursemanagement.mapper.PageMapper;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.log.annotation.Loggable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.nishant.coursemanagement.log.annotation.LogLevel.DEBUG;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final ExceptionUtil exceptionUtil;

    @Cacheable(sync = true, value = "courseById", key = "#id")
    @Loggable(
            action = "QUERY_GET_COURSE_RESPONSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public CourseResponse getCourseResponseById(Long id) {
        return CourseMapper.toResponse(getCourseById(id));
    }

    @Cacheable(sync = true, value = "activeCourseById", key = "#id")
    @Loggable(
            action = "QUERY_GET_ACTIVE_COURSE_RESPONSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public CourseResponse getActiveCourseResponse(Long id) {
        return CourseMapper.toResponse(getActiveCourse(id));
    }

    @Loggable(
            action = "QUERY_GET_COURSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public Course getCourseById(Long id) {
        return (courseRepository.findById(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found")));
    }

    @Loggable(
            action = "QUERY_GET_COURSE_FOR_UPDATE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public Optional<Course> getCourseByIdForUpdate(Long id) {
        return courseRepository.findByIdForUpdate(id);
    }

    @Loggable(
            action = "QUERY_GET_ACTIVE_COURSE",
            extras = {"#id"},
            extraKeys = {"courseId"},
            level = DEBUG
    )
    public Course getActiveCourse(Long id) {
        return courseRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found"));
    }

    @Cacheable(sync = true,
            value = "courses",
            key = "@cacheKeyUtil.buildCourseKey(#title, #isActive, #instructorId, #pageable)"
    )
    @Loggable(
            action = "QUERY_GET_ALL_COURSES",
            extras = {"#title", "#isActive", "#instructorId", "#pageable.getPageNumber()", "#pageable.getPageSize()"},
            extraKeys = {"title", "isActive", "instructorId", "pageNumber", "pageSize"},
            level = DEBUG
    )
    public PageResponse<CourseResponse> getAllCourses(String title, Boolean isActive, Long instructorId, Pageable pageable) {
        return PageMapper.map(
                courseRepository.findCourses(title, isActive, instructorId, pageable),
                CourseMapper::toResponse);
    }
}
