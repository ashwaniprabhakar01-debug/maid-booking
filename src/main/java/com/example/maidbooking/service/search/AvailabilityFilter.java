package com.example.maidbooking.service.search;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.MaidSearchRequest;
import com.example.maidbooking.service.AvailabilityChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailabilityFilter implements MaidFilter {

    private final AvailabilityChecker availabilityChecker;

    @Override
    public boolean matches(Maid maid, MaidSearchRequest request) {
        TimeSlot slot = request.slot();
        return slot == null || availabilityChecker.isAvailable(maid, slot);
    }
}
