package com.nishant.coursemanagement.dto.course;

import com.nishant.coursemanagement.dto.common.SortableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Schema(description = "Available sort fields for courses")
public enum CourseSortBy implements SortableField {
    ID("id"),
    TITLE("title"),
    CREATED_AT("createdAt");

    private final String field;
}