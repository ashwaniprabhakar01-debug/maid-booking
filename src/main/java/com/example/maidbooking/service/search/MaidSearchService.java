package com.example.maidbooking.service.search;

import com.example.maidbooking.dto.request.MaidSearchRequest;
import com.example.maidbooking.dto.response.MaidResponse;
import com.example.maidbooking.repository.MaidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaidSearchService {

    private final MaidRepository maidRepository;
    private final List<MaidFilter> filters;

    public List<MaidResponse> search(MaidSearchRequest request) {
        request.validate();
        List<MaidResponse> results = maidRepository.findAll().stream()
                .filter(maid -> filters.stream().allMatch(filter -> filter.matches(maid, request)))
                .map(MaidResponse::from)
                .toList();
        log.debug("Maid search returned {} result(s)", results.size());
        return results;
    }
}
