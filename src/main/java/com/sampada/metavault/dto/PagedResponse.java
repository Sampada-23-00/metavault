package com.sampada.metavault.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Clean paginated response wrapper.
 * Spring's built-in Page<T> serializes with lots of noisy internal fields.
 * This gives a clean, predictable JSON shape every time.
 *
 * Example:
 * {
 *   "data": [ {...}, {...} ],
 *   "currentPage": 0,
 *   "pageSize": 10,
 *   "totalItems": 42,
 *   "totalPages": 5,
 *   "last": false
 * }
 *
 * Generic <T>: works for records, versions, audit logs — any type.
 */
@Data
@Builder
public class PagedResponse<T> {

    private List<T> data;
    private int currentPage;
    private int pageSize;
    private long totalItems;
    private int totalPages;
    private boolean last;

    /**
     * Static factory: create a PagedResponse from any Spring Data Page.
     * Usage: PagedResponse.from(page, page.getContent())
     *
     * We separate the source page from the mapped content because the
     * content type may differ (e.g., Page<MetadataRecord> → List<RecordResponse>)
     */
    public static <T> PagedResponse<T> from(Page<?> page, List<T> mappedContent) {
        return PagedResponse.<T>builder()
                .data(mappedContent)
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
