package com.nishant.coursemanagement.controller.enrollment;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentSearchRequest;
import com.nishant.coursemanagement.service.enrollment.EnrollmentService;
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
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollments", description = "Enrollment management APIs")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/{courseId}")
    @Operation(summary = "Enroll in course", description = "Enroll authenticated student in a course")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrollment created"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "409", description = "Already enrolled or no seats")
    })
    public EnrollmentResponse enroll(@PathVariable Long courseId) {
        return enrollmentService.enroll(courseId);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    @Operation(summary = "Get my enrollments", description = "Retrieve paginated enrollments for the authenticated student")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public PageResponse<EnrollmentResponse> myEnrollments(@Valid @ParameterObject @ModelAttribute EnrollmentSearchRequest request) {
        return enrollmentService.getMyEnrollments(request);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unenroll from course", description = "Remove authenticated student enrollment from a course")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Unenrolled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    public void unenroll(@PathVariable Long courseId) {
        enrollmentService.unenroll(courseId);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @GetMapping("/{id}")
    @Operation(summary = "Get enrollments by course", description = "Retrieve paginated enrollments for a specific course (INSTRUCTOR only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successful response"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    public PageResponse<EnrollmentResponse> getByCourse(
            @PathVariable Long id,
            @Valid @ParameterObject @ModelAttribute EnrollmentSearchRequest request) {
        return enrollmentService.getEnrollmentsByCourse(id, request);
    }
}
