package com.example.maidbooking.service;

import com.example.maidbooking.domain.Booking;
import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.domain.Occurrence;
import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.CreateBookingRequest;
import com.example.maidbooking.dto.response.BookingResponse;
import com.example.maidbooking.exception.ResourceNotFoundException;
import com.example.maidbooking.exception.SlotUnavailableException;
import com.example.maidbooking.repository.BookingRepository;
import com.example.maidbooking.service.slot.SlotGeneratorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final MaidService maidService;
    private final BookingRepository bookingRepository;
    private final SlotGeneratorRegistry slotGeneratorRegistry;
    private final AvailabilityChecker availabilityChecker;
    private final MaidLocks maidLocks;

    public BookingResponse book(CreateBookingRequest request) {
        Maid maid = maidService.getMaid(request.getMaidId());
        if (!maid.offers(request.getServiceType())) {
            throw new IllegalArgumentException("Maid does not offer " + request.getServiceType());
        }

        List<TimeSlot> slots = slotGeneratorRegistry.get(request.getBookingType()).generate(request);

        return maidLocks.withLock(maid.getId(), () -> {
            slots.forEach(slot -> ensureAvailable(maid, slot));
            Booking booking = bookingRepository.save(createBooking(request, maid, slots));
            log.info("{} booking {} created for maid {} with {} occurrence(s)",
                    booking.getType(), booking.getId(), maid.getId(), booking.getOccurrences().size());
            return BookingResponse.from(booking);
        });
    }

    public Booking getBooking(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    public void save(Booking booking) {
        bookingRepository.save(booking);
    }

    private void ensureAvailable(Maid maid, TimeSlot slot) {
        if (!availabilityChecker.isAvailable(maid, slot)) {
            log.info("Booking rejected: maid {} is not available for {}", maid.getId(), slot);
            throw new SlotUnavailableException("Maid " + maid.getId() + " is not available for " + slot);
        }
    }

    private Booking createBooking(CreateBookingRequest request, Maid maid, List<TimeSlot> slots) {
        List<Occurrence> occurrences = slots.stream()
                .map(slot -> new Occurrence(UUID.randomUUID().toString(), slot, maid.priceFor(slot.duration())))
                .toList();

        return new Booking(UUID.randomUUID().toString(), request.getCustomerId(), maid.getId(),
                request.getServiceType(), request.getBookingType(), occurrences);
    }
}
