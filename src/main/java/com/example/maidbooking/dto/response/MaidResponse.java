package com.example.maidbooking.dto.response;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.enums.Gender;
import com.example.maidbooking.enums.ServiceType;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Getter
public class MaidResponse {

    private final String id;
    private final String name;
    private final Gender gender;
    private final String locality;
    private final Set<ServiceType> services;
    private final BigDecimal pricePerHour;
    private final Double rating;
    private final List<AvailabilityWindowResponse> availability;

    private MaidResponse(Maid maid) {
        this.id = maid.getId();
        this.name = maid.getName();
        this.gender = maid.getGender();
        this.locality = maid.getLocality();
        this.services = maid.getServices();
        this.pricePerHour = maid.getPricePerHour();
        this.rating = maid.getRating();
        this.availability = maid.getAvailability().stream().map(AvailabilityWindowResponse::from).toList();
    }

    public static MaidResponse from(Maid maid) {
        return new MaidResponse(maid);
    }
}
