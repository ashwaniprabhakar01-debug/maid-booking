package com.example.maidbooking.dto.response;

import com.example.maidbooking.domain.Booking;
import com.example.maidbooking.enums.BookingStatus;
import com.example.maidbooking.enums.BookingType;
import com.example.maidbooking.enums.ServiceType;
import lombok.Getter;

import java.util.List;

@Getter
public class BookingResponse {

    private final String id;
    private final String customerId;
    private final String maidId;
    private final ServiceType serviceType;
    private final BookingType type;
    private final BookingStatus status;
    private final List<OccurrenceResponse> occurrences;

    private BookingResponse(Booking booking) {
        this.id = booking.getId();
        this.customerId = booking.getCustomerId();
        this.maidId = booking.getMaidId();
        this.serviceType = booking.getServiceType();
        this.type = booking.getType();
        this.status = booking.getStatus();
        this.occurrences = booking.getOccurrences().stream().map(OccurrenceResponse::from).toList();
    }

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking);
    }
}
