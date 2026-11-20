package com.example.maidbooking.service;

import com.example.maidbooking.dto.request.AvailabilityWindowRequest;
import com.example.maidbooking.dto.request.CreateMaidRequest;
import com.example.maidbooking.dto.response.MaidResponse;
import com.example.maidbooking.enums.Gender;
import com.example.maidbooking.enums.ServiceType;
import com.example.maidbooking.exception.ResourceNotFoundException;
import com.example.maidbooking.repository.inmemory.InMemoryMaidRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaidServiceTest {

    private final MaidService maidService = new MaidService(new InMemoryMaidRepository());

    @Test
    void addedMaidCanBeFetchedById() {
        MaidResponse saved = maidService.addMaid(request());

        assertNotNull(saved.getId());
        assertEquals("Sunita", maidService.getMaid(saved.getId()).getName());
    }

    @Test
    void unknownMaidThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> maidService.getMaid("missing"));
    }

    private CreateMaidRequest request() {
        AvailabilityWindowRequest window = new AvailabilityWindowRequest();
        window.setDayOfWeek(DayOfWeek.MONDAY);
        window.setStartTime(LocalTime.of(8, 0));
        window.setEndTime(LocalTime.of(18, 0));

        CreateMaidRequest request = new CreateMaidRequest();
        request.setName("Sunita");
        request.setGender(Gender.FEMALE);
        request.setLocality("Koramangala");
        request.setServices(Set.of(ServiceType.CLEANING, ServiceType.COOKING));
        request.setPricePerHour(BigDecimal.valueOf(250));
        request.setAvailability(List.of(window));
        return request;
    }
}
