package com.example.maidbooking.controller;

import com.example.maidbooking.dto.request.CreateBookingRequest;
import com.example.maidbooking.dto.request.PaymentRequest;
import com.example.maidbooking.dto.response.BookingResponse;
import com.example.maidbooking.dto.response.CancellationResponse;
import com.example.maidbooking.dto.response.PaymentResponse;
import com.example.maidbooking.service.BookingService;
import com.example.maidbooking.service.cancellation.CancellationService;
import com.example.maidbooking.service.payment.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.book(request);
    }

    @PostMapping("/{bookingId}/occurrences/{occurrenceId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@PathVariable String bookingId, @PathVariable String occurrenceId,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(bookingId, occurrenceId, request);
    }

    @PostMapping("/{bookingId}/cancel")
    public CancellationResponse cancelBooking(@PathVariable String bookingId) {
        return cancellationService.cancelBooking(bookingId);
    }

    @PostMapping("/{bookingId}/occurrences/{occurrenceId}/cancel")
    public CancellationResponse cancelOccurrence(@PathVariable String bookingId, @PathVariable String occurrenceId) {
        return cancellationService.cancelOccurrence(bookingId, occurrenceId);
    }
}
