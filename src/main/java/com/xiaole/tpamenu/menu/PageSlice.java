package com.xiaole.tpamenu.menu;

import java.util.List;

record PageSlice<T>(int index, int totalPages, List<T> entries) {
    static <T> PageSlice<T> of(List<T> allEntries, int requestedPage, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1");
        }

        int totalPages = Math.max(1, (allEntries.size() + pageSize - 1) / pageSize);
        int index = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int start = index * pageSize;
        int end = Math.min(start + pageSize, allEntries.size());
        List<T> entries = start < end ? List.copyOf(allEntries.subList(start, end)) : List.of();
        return new PageSlice<>(index, totalPages, entries);
    }

    boolean hasPrevious() {
        return index > 0;
    }

    boolean hasNext() {
        return index + 1 < totalPages;
    }
}
