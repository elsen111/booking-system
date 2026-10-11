package com.bookingapi.common.pagination;

import com.bookingapi.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableSanitizer {

    private PageableSanitizer() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedSortProperties, Sort defaultSort) {
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        for (Sort.Order order : sort) {
            if (!allowedSortProperties.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort property: " + order.getProperty()
                        + ". Allowed: " + allowedSortProperties);
            }
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}