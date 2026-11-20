package com.example.maidbooking.service.slot;

import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.CreateBookingRequest;
import com.example.maidbooking.enums.BookingType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InstantSlotGenerator implements SlotGenerator {

    private static final Duration ARRIVAL_TIME = Duration.ofMinutes(30);

    private final Clock clock;

    @Override
    public BookingType type() {
        return BookingType.INSTANT;
    }

    @Override
    public List<TimeSlot> generate(CreateBookingRequest request) {
        LocalDateTime start = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES).plus(ARRIVAL_TIME);
        return List.of(TimeSlot.of(start, request.duration()));
    }
}
