package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.dto.course.CourseUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    CourseResponse createCourse(CourseRequest request);

    CourseResponse getCourseById(Long id);

    CourseResponse getActiveCourse(Long id);

    PageResponse<CourseResponse> getAllCourses(String title, Boolean active, Long instructorId, Pageable pageable);

    PageResponse<CourseResponse> getAllActiveCourses(String title, Long instructorId, Pageable pageable);

    PageResponse<CourseResponse> getMyCourses(String title, Boolean active, Pageable pageable);

    CourseResponse updateCourse(Long id, CourseUpdateRequest request);

    CourseResponse patchCourse(Long id, CoursePatchRequest request);

    void deactivateCourse(Long id);
}
