package com.nishant.coursemanagement.dto.common;

public interface SearchRequest<T extends SortableField> {
    Integer page();
    Integer size();
    T sortBy();
    SortDirection direction();
}
