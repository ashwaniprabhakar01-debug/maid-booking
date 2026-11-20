package com.example.maidbooking.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityWindowTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 11, 30, 0, 0);

    private final AvailabilityWindow mondayWindow =
            new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0));

    private static TimeSlot slot(LocalDateTime start, LocalDateTime end) {
        return new TimeSlot(start, end);
    }

    @Test
    void coversSlotInsideWindow() {
        assertTrue(mondayWindow.covers(slot(MONDAY.withHour(9), MONDAY.withHour(10))));
    }

    @Test
    void coversSlotMatchingWindowExactly() {
        assertTrue(mondayWindow.covers(slot(MONDAY.withHour(8), MONDAY.withHour(18))));
    }

    @Test
    void doesNotCoverSlotStartingTooEarly() {
        assertFalse(mondayWindow.covers(slot(MONDAY.withHour(7).withMinute(30), MONDAY.withHour(9))));
    }

    @Test
    void doesNotCoverSlotEndingTooLate() {
        assertFalse(mondayWindow.covers(slot(MONDAY.withHour(17), MONDAY.withHour(19))));
    }

    @Test
    void doesNotCoverSlotOnAnotherDay() {
        LocalDateTime tuesday = MONDAY.plusDays(1);
        assertFalse(mondayWindow.covers(slot(tuesday.withHour(9), tuesday.withHour(10))));
    }

    @Test
    void doesNotCoverSlotCrossingMidnight() {
        assertFalse(mondayWindow.covers(slot(MONDAY.withHour(23), MONDAY.plusDays(1).withHour(1))));
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class,
                () -> new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(8, 0)));
    }
}
