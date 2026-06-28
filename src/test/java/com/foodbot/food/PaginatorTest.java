package com.foodbot.food;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginatorTest {

    @Test
    void totalPagesIsAtLeastOneEvenWhenEmpty() {
        assertEquals(1, Paginator.totalPages(0, 10));
    }

    @Test
    void totalPagesRoundsUp() {
        assertEquals(1, Paginator.totalPages(9, 10));
        assertEquals(1, Paginator.totalPages(10, 10));
        assertEquals(2, Paginator.totalPages(11, 10));
        assertEquals(3, Paginator.totalPages(21, 10));
    }

    @Test
    void clampPageKeepsRequestedPageWhenInRange() {
        assertEquals(2, Paginator.clampPage(2, 5));
    }

    @Test
    void clampPageClampsNegativeAndOutOfRangePages() {
        assertEquals(0, Paginator.clampPage(-3, 5));
        assertEquals(4, Paginator.clampPage(99, 5));
    }

    @Test
    void pageSliceReturnsCorrectWindow() {
        List<Integer> items = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertEquals(List.of(0, 1, 2), Paginator.pageSlice(items, 0, 3));
        assertEquals(List.of(3, 4, 5), Paginator.pageSlice(items, 1, 3));
        assertEquals(List.of(9), Paginator.pageSlice(items, 3, 3));
    }

    @Test
    void pageSliceBeyondRangeReturnsEmpty() {
        List<Integer> items = List.of(0, 1, 2);
        assertTrue(Paginator.pageSlice(items, 5, 3).isEmpty());
    }

    @Test
    void manyItemsRequireMultiplePagesAndEachPageIsReachable() {
        List<Integer> items = new java.util.ArrayList<>();
        for (int i = 0; i < 23; i++) {
            items.add(i);
        }
        int perPage = 10;
        int total = Paginator.totalPages(items.size(), perPage);
        assertEquals(3, total);

        int seen = 0;
        for (int page = 0; page < total; page++) {
            seen += Paginator.pageSlice(items, page, perPage).size();
        }
        assertEquals(items.size(), seen);
    }
}
