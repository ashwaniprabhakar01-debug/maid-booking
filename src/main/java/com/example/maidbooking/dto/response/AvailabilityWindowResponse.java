package com.example.maidbooking.dto.response;

import com.example.maidbooking.domain.AvailabilityWindow;
import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
public class AvailabilityWindowResponse {

    private final DayOfWeek dayOfWeek;
    private final LocalTime startTime;
    private final LocalTime endTime;

    private AvailabilityWindowResponse(AvailabilityWindow window) {
        this.dayOfWeek = window.getDayOfWeek();
        this.startTime = window.getStartTime();
        this.endTime = window.getEndTime();
    }

    public static AvailabilityWindowResponse from(AvailabilityWindow window) {
        return new AvailabilityWindowResponse(window);
    }
}
