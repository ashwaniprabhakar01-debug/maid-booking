package com.example.maidbooking.domain;

import com.example.maidbooking.enums.BookingStatus;
import com.example.maidbooking.enums.BookingType;
import com.example.maidbooking.enums.ServiceType;
import com.example.maidbooking.exception.ResourceNotFoundException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Booking {

    private final String id;
    private final String customerId;
    private final String maidId;
    private final ServiceType serviceType;
    private final BookingType type;
    private final List<Occurrence> occurrences;

    public Booking(String id, String customerId, String maidId, ServiceType serviceType,
                   BookingType type, List<Occurrence> occurrences) {
        if (occurrences == null || occurrences.isEmpty()) {
            throw new IllegalArgumentException("Booking must have at least one occurrence");
        }
        this.id = id;
        this.customerId = customerId;
        this.maidId = maidId;
        this.serviceType = serviceType;
        this.type = type;
        this.occurrences = List.copyOf(occurrences);
    }

    public Occurrence findOccurrence(String occurrenceId) {
        return occurrences.stream()
                .filter(occurrence -> occurrence.getId().equals(occurrenceId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found: " + occurrenceId));
    }

    public List<Occurrence> upcomingOccurrences(LocalDateTime now) {
        return activeOccurrences().stream()
                .filter(occurrence -> occurrence.startsAfter(now))
                .toList();
    }

    public boolean isBusyDuring(TimeSlot slot) {
        return activeOccurrences().stream()
                .anyMatch(occurrence -> occurrence.getSlot().overlaps(slot));
    }

    public BookingStatus getStatus() {
        return activeOccurrences().isEmpty() ? BookingStatus.CANCELLED : BookingStatus.ACTIVE;
    }

    private List<Occurrence> activeOccurrences() {
        return occurrences.stream().filter(Occurrence::isActive).toList();
    }
}
