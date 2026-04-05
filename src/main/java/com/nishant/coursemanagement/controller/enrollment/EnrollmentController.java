package com.nishant.coursemanagement.controller.enrollment;

import com.nishant.coursemanagement.dto.common.PageResponse;
import com.nishant.coursemanagement.dto.enrollment.EnrollmentResponse;
import com.nishant.coursemanagement.service.enrollment.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/{courseId}")
    public EnrollmentResponse enroll(@PathVariable Long courseId) {
        return enrollmentService.enroll(courseId);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/my")
    public PageResponse<EnrollmentResponse> myEnrollments(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 5, sort = "id") Pageable pageable) {
        return enrollmentService.getMyEnrollments(active, pageable);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable Long courseId) {
        enrollmentService.unenroll(courseId);
    }

    @PreAuthorize("hasRole('INSTRUCTOR')")
    @GetMapping("/{id}")
    public PageResponse<EnrollmentResponse> getByCourse(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 5, sort = "id")
            @PathVariable Long id, Pageable pageable) {
        return enrollmentService.getEnrollmentsByCourse(id, active, pageable);
    }
}
