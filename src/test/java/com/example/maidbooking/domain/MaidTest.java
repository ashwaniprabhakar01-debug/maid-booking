package com.example.maidbooking.domain;

import com.example.maidbooking.enums.Gender;
import com.example.maidbooking.enums.ServiceType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaidTest {

    private static final LocalDateTime MONDAY = LocalDateTime.of(2026, 11, 30, 0, 0);

    private final List<AvailabilityWindow> availability = List.of(
            new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
            new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(19, 0)));

    private Maid maid() {
        return new Maid("m1", "Sunita", Gender.FEMALE, "Koramangala",
                Set.of(ServiceType.CLEANING), BigDecimal.valueOf(200), 4.5, availability);
    }

    @Test
    void doesNotWorkDuringSlotSpanningGapBetweenWindows() {
        assertFalse(maid().worksDuring(new TimeSlot(MONDAY.withHour(11), MONDAY.withHour(16))));
    }

    @Test
    void rejectsOverlappingWindowsOnSameDay() {
        assertThrows(IllegalArgumentException.class, () -> maidWith(
                new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(14, 0))));
    }

    @Test
    void rejectsTouchingWindowsOnSameDay() {
        assertThrows(IllegalArgumentException.class, () -> maidWith(
                new AvailabilityWindow(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new AvailabilityWindow(DayOfWeek.TUESDAY, LocalTime.of(12, 0), LocalTime.of(18, 0))));
    }

    @Test
    void allowsSameHoursOnDifferentDays() {
        maidWith(
                new AvailabilityWindow(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)),
                new AvailabilityWindow(DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(18, 0)));
    }

    private Maid maidWith(AvailabilityWindow... windows) {
        return new Maid("m1", "Sunita", Gender.FEMALE, "Koramangala",
                Set.of(ServiceType.CLEANING), BigDecimal.valueOf(200), 4.5, List.of(windows));
    }
}
