package com.example.maidbooking.repository.inmemory;

import com.example.maidbooking.domain.Booking;
import com.example.maidbooking.enums.OccurrenceStatus;

import java.util.HashMap;
import java.util.Map;

public class StatusRecordingBookingRepository extends InMemoryBookingRepository {

    private final Map<String, OccurrenceStatus> savedStatuses = new HashMap<>();

    @Override
    public Booking save(Booking booking) {
        booking.getOccurrences().forEach(occurrence -> savedStatuses.put(occurrence.getId(), occurrence.getStatus()));
        return super.save(booking);
    }

    public OccurrenceStatus savedStatus(String occurrenceId) {
        return savedStatuses.get(occurrenceId);
    }
}
