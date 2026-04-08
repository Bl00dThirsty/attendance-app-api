package com.example.attendance_app.service.support;

import com.example.attendance_app.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageQuerySupport {

    private static final int MAX_PAGE_SIZE = 200;

    private PageQuerySupport() {
    }

    public static Pageable buildPageable(
        int page,
        int size,
        String sortBy,
        String sortDir,
        Set<String> allowedSortFields,
        String defaultSortBy,
        Sort.Direction defaultDirection
    ) {
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        String resolvedSortBy = resolveSortBy(sortBy, defaultSortBy, allowedSortFields);
        Sort.Direction direction = resolveDirection(sortDir, defaultDirection);

        return PageRequest.of(page, size, Sort.by(direction, resolvedSortBy));
    }

    private static String resolveSortBy(String sortBy, String defaultSortBy, Set<String> allowedSortFields) {
        String resolved = sortBy == null || sortBy.isBlank() ? defaultSortBy : sortBy.trim();
        if (!allowedSortFields.contains(resolved)) {
            throw new BadRequestException("Unsupported sortBy value: " + resolved);
        }
        return resolved;
    }

    private static Sort.Direction resolveDirection(String sortDir, Sort.Direction defaultDirection) {
        if (sortDir == null || sortDir.isBlank()) {
            return defaultDirection;
        }
        try {
            return Sort.Direction.fromString(sortDir.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unsupported sortDir value: " + sortDir);
        }
    }
}
