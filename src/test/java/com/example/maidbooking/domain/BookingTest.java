package com.example.maidbooking.domain;

import com.example.maidbooking.enums.BookingStatus;
import com.example.maidbooking.enums.BookingType;
import com.example.maidbooking.enums.ServiceType;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 10, 0);

    private final Occurrence first = occurrence("o1", 1);
    private final Occurrence second = occurrence("o2", 8);
    private final Booking booking = new Booking("b1", "c1", "m1", ServiceType.CLEANING,
            BookingType.RECURRING, List.of(first, second));

    private static Occurrence occurrence(String id, int day) {
        return new Occurrence(id, slot(day, 9, 11), BigDecimal.valueOf(500));
    }

    private static TimeSlot slot(int day, int fromHour, int toHour) {
        return new TimeSlot(LocalDateTime.of(2026, 12, day, fromHour, 0),
                LocalDateTime.of(2026, 12, day, toHour, 0));
    }

    @Test
    void isBusyDuringOverlappingSlot() {
        assertTrue(booking.isBusyDuring(slot(8, 10, 12)));
    }

    @Test
    void cancelledOccurrenceFreesTheSlot() {
        second.cancel(NOW);
        assertFalse(booking.isBusyDuring(slot(8, 10, 12)));
    }

    @Test
    void upcomingOccurrencesExcludeCancelledAndStartedOnes() {
        first.cancel(NOW);
        assertEquals(List.of(second), booking.upcomingOccurrences(NOW));

        assertEquals(List.of(), booking.upcomingOccurrences(LocalDateTime.of(2026, 12, 8, 9, 0)));
    }

    @Test
    void bookingStaysActiveWhileAnyOccurrenceIsActive() {
        first.cancel(NOW);
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
    }

    @Test
    void bookingIsCancelledWhenAllOccurrencesAreCancelled() {
        first.cancel(NOW);
        second.cancel(NOW);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }
}
