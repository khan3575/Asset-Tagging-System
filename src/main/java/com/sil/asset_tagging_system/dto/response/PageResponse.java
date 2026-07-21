package com.sil.asset_tagging_system.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;


/**
 * Standard pagination response used by
 * paginated REST API endpoints.
 *
 * @param content       records available on the current page
 * @param pageNumber    zero-based current page number
 * @param pageSize      maximum records requested per page
 * @param totalElements total number of matching records
 * @param totalPages    total number of available pages
 * @param first         indicates whether this is the first page
 * @param last          indicates whether this is the final page
 * @param empty         indicates whether the page has no records
 */
public record PageResponse<T>(

        List<T> content,

        int pageNumber,

        int pageSize,

        long totalElements,

        int totalPages,

        boolean first,

        boolean last,

        boolean empty

) {

    /**
     * Converts a Spring Data Page into the
     * application's standard pagination response.
     */
    public static <T> PageResponse<T> from(
            Page<T> page
    ) {

        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }
}