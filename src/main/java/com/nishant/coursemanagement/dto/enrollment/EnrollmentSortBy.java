package com.nishant.coursemanagement.dto.enrollment;

import com.nishant.coursemanagement.dto.common.SortableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Schema(description = "Available sort fields for enrollments")
public enum EnrollmentSortBy implements SortableField {
    ID("id"),
    CREATED_AT("createdAt");

    private final String field;
}