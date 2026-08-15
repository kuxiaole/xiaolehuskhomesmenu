package com.xiaole.tpamenu.menu;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageSliceTest {
    @Test
    void emptyListStillHasOnePage() {
        PageSlice<Integer> page = PageSlice.of(List.of(), 0, 45);

        assertEquals(0, page.index());
        assertEquals(1, page.totalPages());
        assertTrue(page.entries().isEmpty());
        assertFalse(page.hasPrevious());
        assertFalse(page.hasNext());
    }

    @Test
    void exactlyFortyFiveEntriesFitOnOnePage() {
        PageSlice<Integer> page = PageSlice.of(entries(45), 0, 45);

        assertEquals(1, page.totalPages());
        assertEquals(45, page.entries().size());
        assertFalse(page.hasNext());
    }

    @Test
    void fortySixthEntryAppearsOnSecondPage() {
        PageSlice<Integer> page = PageSlice.of(entries(46), 1, 45);

        assertEquals(1, page.index());
        assertEquals(2, page.totalPages());
        assertEquals(List.of(45), page.entries());
        assertTrue(page.hasPrevious());
        assertFalse(page.hasNext());
    }

    @Test
    void oversizedPageRequestClampsToLastPageAfterPlayersLeave() {
        PageSlice<Integer> page = PageSlice.of(entries(12), 8, 10);

        assertEquals(1, page.index());
        assertEquals(2, page.totalPages());
        assertEquals(List.of(10, 11), page.entries());
    }

    @Test
    void ninetyOneEntriesCanNavigateAcrossThreePages() {
        PageSlice<Integer> first = PageSlice.of(entries(91), 0, 45);
        PageSlice<Integer> second = PageSlice.of(entries(91), 1, 45);
        PageSlice<Integer> third = PageSlice.of(entries(91), 2, 45);

        assertEquals(3, first.totalPages());
        assertEquals(45, first.entries().size());
        assertFalse(first.hasPrevious());
        assertTrue(first.hasNext());

        assertEquals(45, second.entries().size());
        assertTrue(second.hasPrevious());
        assertTrue(second.hasNext());

        assertEquals(List.of(90), third.entries());
        assertTrue(third.hasPrevious());
        assertFalse(third.hasNext());
    }

    @Test
    void pageSizeMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> PageSlice.of(entries(1), 0, 0));
    }

    private List<Integer> entries(int size) {
        return IntStream.range(0, size).boxed().toList();
    }
}
