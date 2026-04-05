package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.entity.Course;
import com.nishant.coursemanagement.exception.ExceptionUtil;
import com.nishant.coursemanagement.mapper.CourseMapper;
import com.nishant.coursemanagement.mapper.PageMapper;
import com.nishant.coursemanagement.repository.course.CourseRepository;
import com.nishant.coursemanagement.util.LogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final ExceptionUtil exceptionUtil;

    @Cacheable(sync = true, value = "courseById", key = "#id")
    public CourseResponse getCourseResponseById(Long id) {
        try {
            LogUtil.put("action", "QUERY_GET_COURSE_RESPONSE");
            LogUtil.put("courseId", id);
            log.debug("Getting course response");
        } finally {
            LogUtil.clear();
        }
        return CourseMapper.toResponse(getCourseById(id));
    }

    @Cacheable(sync = true, value = "activeCourseById", key = "#id")
    public CourseResponse getActiveCourseResponse(Long id) {
        try {
            LogUtil.put("action", "QUERY_GET_ACTIVE_COURSE_RESPONSE");
            LogUtil.put("courseId", id);
            log.debug("Getting active course response");
        } finally {
            LogUtil.clear();
        }
        return CourseMapper.toResponse(getActiveCourse(id));
    }

    public Course getCourseById(Long id) {
        try {
            LogUtil.put("action", "QUERY_GET_COURSE");
            LogUtil.put("courseId", id);
            log.debug("Getting course");
        } finally {
            LogUtil.clear();
        }
        return (courseRepository.findById(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found")));
    }

    public Optional<Course> getCourseByIdForUpdate(Long id) {
        try {
            LogUtil.put("action", "QUERY_GET_COURSE_FOR_UPDATE");
            LogUtil.put("courseId", id);
            log.debug("Getting course for update");
        } finally {
            LogUtil.clear();
        }
        return courseRepository.findByIdForUpdate(id);
    }

    public Course getActiveCourse(Long id) {
        try {
            LogUtil.put("action", "QUERY_GET_ACTIVE_COURSE");
            LogUtil.put("courseId", id);
            log.debug("Getting active course");
        } finally {
            LogUtil.clear();
        }
        return courseRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> exceptionUtil.notFound("Course not found"));
    }


    @Cacheable(sync = true,
            value = "courses",
            key = "@cacheKeyUtil.buildCourseKey(#title, #active, #instructorId, #pageable)"
    )
    public PageResponse<CourseResponse> getAllCourses(String title, Boolean active, Long instructorId, Pageable pageable) {
        try {
            LogUtil.put("action", "QUERY_GET_ALL_COURSES");
            LogUtil.put("title", title);
            LogUtil.put("active", active);
            LogUtil.put("instructorId", instructorId);
            LogUtil.put("pageNumber", pageable.getPageNumber());
            LogUtil.put("pageSize", pageable.getPageSize());
            log.debug("Getting all courses");
        } finally {
            LogUtil.clear();
        }
        return PageMapper.map(
                courseRepository.findCourses(title, active, instructorId, pageable),
                CourseMapper::toResponse);
    }
}
