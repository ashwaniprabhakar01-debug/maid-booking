package com.example.maidbooking.service.slot;

import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.CreateBookingRequest;
import com.example.maidbooking.enums.BookingType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecurringSlotGenerator implements SlotGenerator {

    private static final int MAX_SPAN_DAYS = 90;

    private final Clock clock;

    @Override
    public BookingType type() {
        return BookingType.RECURRING;
    }

    @Override
    public List<TimeSlot> generate(CreateBookingRequest request) {
        validate(request);
        LocalDateTime now = LocalDateTime.now(clock);

        List<TimeSlot> slots = request.getStartDate().datesUntil(request.getEndDate().plusDays(1))
                .filter(date -> request.getDaysOfWeek().contains(date.getDayOfWeek()))
                .map(date -> date.atTime(request.getTimeOfDay()))
                .filter(start -> start.isAfter(now))
                .map(start -> TimeSlot.of(start, request.duration()))
                .toList();

        if (slots.isEmpty()) {
            throw new IllegalArgumentException("No upcoming occurrences between startDate and endDate");
        }
        return slots;
    }

    private void validate(CreateBookingRequest request) {
        if (request.getDaysOfWeek() == null || request.getDaysOfWeek().isEmpty()) {
            throw new IllegalArgumentException("daysOfWeek is required for a recurring booking");
        }
        if (request.getTimeOfDay() == null || request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("timeOfDay, startDate and endDate are required for a recurring booking");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (request.getEndDate().isAfter(request.getStartDate().plusDays(MAX_SPAN_DAYS))) {
            throw new IllegalArgumentException("Recurring bookings can span at most " + MAX_SPAN_DAYS + " days");
        }
    }
}
