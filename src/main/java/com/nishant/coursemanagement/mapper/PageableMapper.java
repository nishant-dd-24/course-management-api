package com.nishant.coursemanagement.mapper;

import com.nishant.coursemanagement.dto.common.SearchRequest;
import com.nishant.coursemanagement.dto.common.SortDirection;
import com.nishant.coursemanagement.dto.common.SortableField;
import org.springframework.data.domain.*;

public class PageableMapper {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 5;
    private static final int MAX_SIZE = 50;

    public static <T extends SortableField> Pageable toPageable(SearchRequest<T> request){
        if (request != null) {
            return toPageable(
                    request.page(),
                    request.size(),
                    request.sortBy(),
                    request.direction()
            );
        } else {
            return PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);
        }
    }

    public static <T extends SortableField> Pageable toPageable(
            Integer page,
            Integer size,
            T sortBy,
            SortDirection direction
    ) {

        int resolvedPage = page != null ? page : DEFAULT_PAGE;
        int resolvedSize = size != null ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        Sort sort = Sort.unsorted();

        if (sortBy != null) {
            Sort.Direction dir =
                    direction == SortDirection.DESC
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC;

            sort = Sort.by(dir, sortBy.getField());
        }

        return PageRequest.of(resolvedPage, resolvedSize, sort);
    }
}