package com.nishant.coursemanagement.dto.user;

import com.nishant.coursemanagement.dto.common.SortableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Schema(description = "Available sort fields for users")
public enum UserSortBy implements SortableField {
    ID("id"),
    NAME("name"),
    EMAIL("email"),
    CREATED_AT("createdAt");

    private final String field;
}