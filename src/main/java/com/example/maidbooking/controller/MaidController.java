package com.example.maidbooking.controller;

import com.example.maidbooking.dto.request.CreateMaidRequest;
import com.example.maidbooking.dto.request.MaidSearchRequest;
import com.example.maidbooking.dto.response.MaidResponse;
import com.example.maidbooking.service.MaidService;
import com.example.maidbooking.service.search.MaidSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/maids")
@RequiredArgsConstructor
public class MaidController {

    private final MaidService maidService;
    private final MaidSearchService maidSearchService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaidResponse addMaid(@Valid @RequestBody CreateMaidRequest request) {
        return maidService.addMaid(request);
    }

    @GetMapping("/search")
    public List<MaidResponse> search(@ModelAttribute MaidSearchRequest maidSearchRequest) {
        return maidSearchService.search(maidSearchRequest);
    }
}
