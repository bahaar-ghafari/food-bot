package com.foodbot.food;

import java.util.List;

public final class Paginator {

    public static int totalPages(int itemCount, int perPage) {
        return Math.max(1, (itemCount + perPage - 1) / perPage);
    }

    public static int clampPage(int page, int totalPages) {
        return Math.max(0, Math.min(page, totalPages - 1));
    }

    public static <T> List<T> pageSlice(List<T> items, int page, int perPage) {
        int start = page * perPage;
        if (start >= items.size()) {
            return List.of();
        }
        int end = Math.min(start + perPage, items.size());
        return items.subList(start, end);
    }

    private Paginator() {
    }
}
