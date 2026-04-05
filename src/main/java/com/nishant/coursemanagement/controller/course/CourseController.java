package com.nishant.coursemanagement.controller.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.CoursePatchRequest;
import com.nishant.coursemanagement.dto.course.CourseRequest;
import com.nishant.coursemanagement.dto.course.CourseResponse;
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
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long instructorId,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return courseService.getAllCourses(title, active, instructorId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseResponse getCourse(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @GetMapping("/active/{id}")
    public CourseResponse getActiveCourse(@PathVariable Long id) {
        return courseService.getActiveCourse(id);
    }

    @GetMapping("/available-courses")
    public PageResponse<CourseResponse> allActiveCourses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long instructorId,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return courseService.getAllActiveCourses(title, instructorId, pageable);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @GetMapping("/my")
    public PageResponse<CourseResponse> myCourses(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return courseService.getMyCourses(title, active, pageable);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return courseService.updateCourse(id, request);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @PatchMapping("/{id}")
    public CourseResponse patch(@PathVariable Long id, @Valid @RequestBody CoursePatchRequest request) {
        return courseService.patchCourse(id, request);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        courseService.deactivateCourse(id);
    }

}
