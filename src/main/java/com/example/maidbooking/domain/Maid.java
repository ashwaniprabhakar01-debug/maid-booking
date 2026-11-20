package com.example.maidbooking.domain;

import com.example.maidbooking.enums.Gender;
import com.example.maidbooking.enums.ServiceType;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Getter
public class Maid {

    private final String id;
    private final String name;
    private final Gender gender;
    private final String locality;
    private final Set<ServiceType> services;
    private final BigDecimal pricePerHour;
    private final Double rating;
    private final List<AvailabilityWindow> availability;

    public Maid(String id, String name, Gender gender, String locality, Set<ServiceType> services,
                BigDecimal pricePerHour, Double rating, List<AvailabilityWindow> availability) {
        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("Maid must offer at least one service");
        }
        if (pricePerHour == null || pricePerHour.signum() <= 0) {
            throw new IllegalArgumentException("Price per hour must be positive");
        }
        if (rating != null && (rating < 0 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
        if (availability == null || availability.isEmpty()) {
            throw new IllegalArgumentException("Maid must have at least one availability window");
        }
        ensureNoOverlappingWindows(availability);
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.locality = locality;
        this.services = Set.copyOf(services);
        this.pricePerHour = pricePerHour;
        this.rating = rating;
        this.availability = List.copyOf(availability);
    }

    private static void ensureNoOverlappingWindows(List<AvailabilityWindow> availability) {
        for (int i = 0; i < availability.size(); i++) {
            for (int j = i + 1; j < availability.size(); j++) {
                if (availability.get(i).overlapsOrTouches(availability.get(j))) {
                    throw new IllegalArgumentException(
                            "Availability windows on the same day must not overlap or touch; merge them into one window");
                }
            }
        }
    }

    public boolean offers(ServiceType serviceType) {
        return services.contains(serviceType);
    }

    public boolean worksDuring(TimeSlot slot) {
        return availability.stream().anyMatch(window -> window.covers(slot));
    }

    public BigDecimal priceFor(Duration duration) {
        return pricePerHour.multiply(BigDecimal.valueOf(duration.toMinutes()))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
