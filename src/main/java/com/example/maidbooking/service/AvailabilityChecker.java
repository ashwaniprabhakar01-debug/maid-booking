package com.example.maidbooking.service;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailabilityChecker {

    private final BookingRepository bookingRepository;

    public boolean isAvailable(Maid maid, TimeSlot slot) {
        return maid.worksDuring(slot)
                && bookingRepository.findByMaidId(maid.getId()).stream()
                        .noneMatch(booking -> booking.isBusyDuring(slot));
    }
}
