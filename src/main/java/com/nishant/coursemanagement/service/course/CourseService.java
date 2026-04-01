package com.nishant.coursemanagement.service.course;

import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    CourseResponse createCourse(CourseRequest request);

    CourseResponse getCourse(Long id);

    CourseResponse getActiveCourse(Long id);

    Page<CourseResponse> getAllCourses(String title, Boolean active, Long instructorId, Pageable pageable);

    Page<CourseResponse> getAllActiveCourses(String title, Long instructorId, Pageable pageable);

    Page<CourseResponse> getMyCourses(String title, Boolean active, Pageable pageable);

    CourseResponse updateCourse(Long id, CourseRequest request);

    CourseResponse patchCourse(Long id, CoursePatchRequest request);

    void deactivateCourse(Long id);
}
