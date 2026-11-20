package com.example.maidbooking.dto.request;

import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.enums.Gender;
import com.example.maidbooking.enums.ServiceType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class MaidSearchRequest {

    private String locality;
    private Set<ServiceType> services;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    private Integer durationMinutes;
    private Double minRating;
    private BigDecimal maxPrice;
    private Gender gender;

    public void validate() {
        boolean onlyOneGiven = (startTime == null) != (durationMinutes == null);
        boolean invalidDuration = durationMinutes != null && durationMinutes <= 0;
        if (onlyOneGiven || invalidDuration) {
            throw new IllegalArgumentException("startTime and a positive durationMinutes must be given together");
        }
    }

    public TimeSlot slot() {
        if (startTime == null) {
            return null;
        }
        return TimeSlot.of(startTime, Duration.ofMinutes(durationMinutes));
    }
}
