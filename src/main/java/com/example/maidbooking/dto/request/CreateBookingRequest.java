package com.example.maidbooking.dto.request;

import com.example.maidbooking.enums.BookingType;
import com.example.maidbooking.enums.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Getter
@Setter
public class CreateBookingRequest {

    @NotBlank
    private String customerId;

    @NotBlank
    private String maidId;

    @NotNull
    private ServiceType serviceType;

    @NotNull
    private BookingType bookingType;

    @NotNull
    @Positive
    private Integer durationMinutes;

    private LocalDateTime startTime;

    private Set<@NotNull DayOfWeek> daysOfWeek;

    private LocalTime timeOfDay;

    private LocalDate startDate;

    private LocalDate endDate;

    public Duration duration() {
        return Duration.ofMinutes(durationMinutes);
    }
}
