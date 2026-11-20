package com.example.maidbooking.service.search;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.dto.request.MaidSearchRequest;
import org.springframework.stereotype.Component;

@Component
public class PriceFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, MaidSearchRequest request) {
        return request.getMaxPrice() == null || maid.getPricePerHour().compareTo(request.getMaxPrice()) <= 0;
    }
}
