package com.nishant.coursemanagement.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Sorting direction")
public enum SortDirection {
    ASC,
    DESC
}