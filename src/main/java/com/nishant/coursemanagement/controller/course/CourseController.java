package com.nishant.coursemanagement.controller.course;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.course.*;
import com.nishant.coursemanagement.service.course.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management APIs")
public class CourseController {

    private final CourseService courseService;

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create course", description = "Create a new course (INSTRUCTOR only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Course created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public CourseResponse create(@Valid @RequestBody CourseRequest request) {
        return courseService.createCourse(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all courses", description = "Retrieve paginated and filtered courses (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public PageResponse<CourseResponse> allCourses(@Valid @ParameterObject @ModelAttribute CourseSearchRequest request) {
        return courseService.getAllCourses(request);
    }

    @GetMapping("/active")
    @Operation(summary = "Get active courses", description = "Retrieve paginated active courses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public PageResponse<CourseResponse> allActiveCourses(@Valid @ParameterObject @ModelAttribute CourseSearchRequest request) {
        return courseService.getAllActiveCourses(request);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get course by ID", description = "Retrieve a course by ID (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public CourseResponse getCourse(@PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @GetMapping("/active/{id:\\d+}")
    @Operation(summary = "Get active course by ID", description = "Retrieve an active course by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public CourseResponse getActiveCourse(@PathVariable Long id) {
        return courseService.getActiveCourse(id);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @GetMapping("/my")
    @Operation(summary = "Get my courses", description = "Retrieve instructor-owned courses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public PageResponse<CourseResponse> myCourses(@Valid @ParameterObject @ModelAttribute CourseSearchRequest request) {
        return courseService.getMyCourses(request);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @PutMapping("/{id:\\d+}")
    @Operation(summary = "Update course", description = "Fully update a course (INSTRUCTOR only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request) {
        return courseService.updateCourse(id, request);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @PatchMapping("/{id:\\d+}")
    @Operation(summary = "Patch course", description = "Partially update a course (INSTRUCTOR only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course updated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public CourseResponse patch(@PathVariable Long id, @Valid @RequestBody CoursePatchRequest request) {
        return courseService.patchCourse(id, request);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate course", description = "Deactivate a course (INSTRUCTOR only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Course deactivated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public void deactivate(@PathVariable Long id) {
        courseService.deactivateCourse(id);
    }

}
