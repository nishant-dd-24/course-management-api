package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.*;

public interface CourseService {

    CourseResponse createCourse(CourseRequest request);

    CourseResponse getCourseById(Long id);

    CourseResponse getActiveCourse(Long id);

    PageResponse<CourseResponse> getAllCourses(CourseSearchRequest request);

    PageResponse<CourseResponse> getAllActiveCourses(CourseSearchRequest request);

    PageResponse<CourseResponse> getMyCourses(CourseSearchRequest request);

    CourseResponse updateCourse(Long id, CourseUpdateRequest request);

    CourseResponse patchCourse(Long id, CoursePatchRequest request);

    void deactivateCourse(Long id);
}
