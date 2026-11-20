package com.example.maidbooking.dto.request;

import com.example.maidbooking.domain.AvailabilityWindow;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class AvailabilityWindowRequest {

    @NotNull
    private DayOfWeek dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    public AvailabilityWindow toDomain() {
        return new AvailabilityWindow(dayOfWeek, startTime, endTime);
    }
}
