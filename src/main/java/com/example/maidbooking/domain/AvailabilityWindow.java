package com.example.maidbooking.domain;

import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
public final class AvailabilityWindow {

    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public AvailabilityWindow(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (dayOfWeek == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Day, start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean covers(TimeSlot slot) {
        boolean sameDay = slot.getStart().toLocalDate().equals(slot.getEnd().toLocalDate());
        return sameDay
                && slot.getStart().getDayOfWeek() == dayOfWeek
                && !slot.getStart().toLocalTime().isBefore(startTime)
                && !slot.getEnd().toLocalTime().isAfter(endTime);
    }

    public boolean overlapsOrTouches(AvailabilityWindow other) {
        return dayOfWeek == other.dayOfWeek
                && !startTime.isAfter(other.endTime)
                && !other.startTime.isAfter(endTime);
    }
}
