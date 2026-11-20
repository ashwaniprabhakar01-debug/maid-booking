package com.example.maidbooking.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSlotTest {

    private static TimeSlot slot(int day, int fromHour, int toHour) {
        return new TimeSlot(
                LocalDateTime.of(2026, 12, day, fromHour, 0),
                LocalDateTime.of(2026, 12, day, toHour, 0));
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () -> slot(1, 11, 10));
    }

    @Test
    void rejectsZeroLengthSlot() {
        assertThrows(IllegalArgumentException.class, () -> slot(1, 10, 10));
    }

    @Test
    void partialOverlap() {
        assertTrue(slot(1, 9, 11).overlaps(slot(1, 10, 12)));
        assertTrue(slot(1, 10, 12).overlaps(slot(1, 9, 11)));
    }

    @Test
    void containedSlotOverlaps() {
        assertTrue(slot(1, 9, 12).overlaps(slot(1, 10, 11)));
        assertTrue(slot(1, 10, 11).overlaps(slot(1, 9, 12)));
    }

    @Test
    void backToBackSlotsDoNotOverlap() {
        assertFalse(slot(1, 9, 10).overlaps(slot(1, 10, 11)));
        assertFalse(slot(1, 10, 11).overlaps(slot(1, 9, 10)));
    }

    @Test
    void sameHoursOnDifferentDaysDoNotOverlap() {
        assertFalse(slot(1, 9, 10).overlaps(slot(2, 9, 10)));
    }
}
