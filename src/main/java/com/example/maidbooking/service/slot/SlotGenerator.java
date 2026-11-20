package com.example.maidbooking.service.slot;

import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.CreateBookingRequest;
import com.example.maidbooking.enums.BookingType;

import java.util.List;

public interface SlotGenerator {

    BookingType type();

    List<TimeSlot> generate(CreateBookingRequest request);
}
