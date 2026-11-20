package com.example.maidbooking.service.slot;

import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.CreateBookingRequest;
import com.example.maidbooking.enums.BookingType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlotGeneratorTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 10, 15, 42);
    private final Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);

    private final InstantSlotGenerator instant = new InstantSlotGenerator(clock);
    private final ScheduledSlotGenerator scheduled = new ScheduledSlotGenerator(clock);
    private final RecurringSlotGenerator recurring = new RecurringSlotGenerator(clock);

    private static CreateBookingRequest request(BookingType type) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setBookingType(type);
        request.setDurationMinutes(120);
        return request;
    }

    @Test
    void instantSlotStartsAfterArrivalTime() {
        List<TimeSlot> slots = instant.generate(request(BookingType.INSTANT));

        assertEquals(List.of(new TimeSlot(
                LocalDateTime.of(2026, 11, 30, 10, 45),
                LocalDateTime.of(2026, 11, 30, 12, 45))), slots);
    }

    @Test
    void scheduledSlotUsesRequestedStartTime() {
        CreateBookingRequest request = request(BookingType.SCHEDULED);
        request.setStartTime(NOW.plusDays(1).withHour(9).withMinute(0).withSecond(0));

        List<TimeSlot> slots = scheduled.generate(request);

        assertEquals(1, slots.size());
        assertEquals(request.getStartTime().plusHours(2), slots.get(0).getEnd());
    }

    @Test
    void scheduledBookingInThePastIsRejected() {
        CreateBookingRequest request = request(BookingType.SCHEDULED);
        request.setStartTime(NOW.minusHours(1));

        assertThrows(IllegalArgumentException.class, () -> scheduled.generate(request));
    }

    @Test
    void scheduledBookingWithoutStartTimeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> scheduled.generate(request(BookingType.SCHEDULED)));
    }

    @Test
    void recurringGeneratesSlotForEachMatchingDayInRange() {
        CreateBookingRequest request = recurringRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 14));

        List<TimeSlot> slots = recurring.generate(request);

        assertEquals(4, slots.size());
        slots.forEach(slot -> assertEquals(LocalTime.of(9, 0), slot.getStart().toLocalTime()));
    }

    @Test
    void recurringSkipsOccurrencesAlreadyInThePast() {
        CreateBookingRequest request = recurringRequest(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 2));

        List<TimeSlot> slots = recurring.generate(request);

        assertEquals(1, slots.size());
        assertEquals(DayOfWeek.WEDNESDAY, slots.get(0).getStart().getDayOfWeek());
    }

    @Test
    void recurringWithEndBeforeStartIsRejected() {
        CreateBookingRequest request = recurringRequest(LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 1));

        assertThrows(IllegalArgumentException.class, () -> recurring.generate(request));
    }

    @Test
    void recurringWithoutDaysIsRejected() {
        CreateBookingRequest request = recurringRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 14));
        request.setDaysOfWeek(Set.of());

        assertThrows(IllegalArgumentException.class, () -> recurring.generate(request));
    }

    @Test
    void recurringSpanningExactlyMaxDaysIsAllowed() {
        CreateBookingRequest request = recurringRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1).plusDays(90));

        assertEquals(26, recurring.generate(request).size());
    }

    @Test
    void recurringSpanningMoreThanMaxDaysIsRejected() {
        CreateBookingRequest request = recurringRequest(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1).plusDays(91));

        assertThrows(IllegalArgumentException.class, () -> recurring.generate(request));
    }

    private static CreateBookingRequest recurringRequest(LocalDate from, LocalDate to) {
        CreateBookingRequest request = request(BookingType.RECURRING);
        request.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        request.setTimeOfDay(LocalTime.of(9, 0));
        request.setStartDate(from);
        request.setEndDate(to);
        return request;
    }
}
