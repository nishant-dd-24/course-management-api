package com.nishant.coursemanagement.controller.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
import com.nishant.coursemanagement.dto.course.CourseUpdateRequest;
import com.nishant.coursemanagement.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return courseService.createCourse(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<CourseResponse> allCourses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long instructorId,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return courseService.getAllCourses(title, isActive, instructorId, pageable);
    }

    @GetMapping("/active")
    public PageResponse<CourseResponse> allActiveCourses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long instructorId,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return courseService.getAllActiveCourses(title, instructorId, pageable);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse getCourse(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @GetMapping("/active/{id:\\d+}")
    public CourseResponse getActiveCourse(@PathVariable Long id) {
        return courseService.getActiveCourse(id);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @GetMapping("/my")
    public PageResponse<CourseResponse> myCourses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return courseService.getMyCourses(title, isActive, pageable);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @PutMapping("/{id:\\d+}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request) {
        return courseService.updateCourse(id, request);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @PatchMapping("/{id:\\d+}")
    public CourseResponse patch(@PathVariable Long id, @Valid @RequestBody CoursePatchRequest request) {
        return courseService.patchCourse(id, request);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        courseService.deactivateCourse(id);
    }

}
