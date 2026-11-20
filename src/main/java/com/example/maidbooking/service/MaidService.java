package com.example.maidbooking.service;

import com.example.maidbooking.domain.AvailabilityWindow;
import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.dto.request.AvailabilityWindowRequest;
import com.example.maidbooking.dto.request.CreateMaidRequest;
import com.example.maidbooking.dto.response.MaidResponse;
import com.example.maidbooking.exception.ResourceNotFoundException;
import com.example.maidbooking.repository.MaidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaidService {

    private final MaidRepository maidRepository;

    public MaidResponse addMaid(CreateMaidRequest request) {
        List<AvailabilityWindow> availability = request.getAvailability().stream()
                .map(AvailabilityWindowRequest::toDomain)
                .toList();

        Maid maid = new Maid(
                UUID.randomUUID().toString(),
                request.getName(),
                request.getGender(),
                request.getLocality(),
                request.getServices(),
                request.getPricePerHour(),
                request.getRating(),
                availability);

        Maid saved = maidRepository.save(maid);
        log.info("Maid {} added in {} offering {}", saved.getId(), saved.getLocality(), saved.getServices());
        return MaidResponse.from(saved);
    }

    public Maid getMaid(String id) {
        return maidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maid not found: " + id));
    }
}
