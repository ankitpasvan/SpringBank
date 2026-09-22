package com.example.banking.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * Stable JSON shape for every paginated endpoint.
 * (We do not return Spring's Page object directly: its JSON layout is not a stable API contract.)
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
