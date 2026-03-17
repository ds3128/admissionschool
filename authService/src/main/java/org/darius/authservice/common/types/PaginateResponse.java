package org.darius.authservice.common.types;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaginateResponse<T> {
    private long count;
    private boolean hasMore;
    private List<T> data;
    private long currentPage;
    private long totalPages;

    public PaginateResponse(long count, boolean hasMore, List<T> data, long currentPage, long totalPages) {
        this.count = count;
        this.hasMore = hasMore;
        this.data = data;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }
}

