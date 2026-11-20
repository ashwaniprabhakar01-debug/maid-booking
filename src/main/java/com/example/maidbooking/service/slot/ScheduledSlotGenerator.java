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
public class ScheduledSlotGenerator implements SlotGenerator {

    private final Clock clock;

    @Override
    public BookingType type() {
        return BookingType.SCHEDULED;
    }

    @Override
    public List<TimeSlot> generate(CreateBookingRequest request) {
        LocalDateTime startTime = request.getStartTime();
        if (startTime == null) {
            throw new IllegalArgumentException("startTime is required for a scheduled booking");
        }
        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException("startTime must be in the future");
        }
        return List.of(TimeSlot.of(startTime, request.duration()));
    }
}
