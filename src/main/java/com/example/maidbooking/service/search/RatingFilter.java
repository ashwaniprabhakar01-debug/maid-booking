package com.example.maidbooking.service.search;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.dto.request.MaidSearchRequest;
import org.springframework.stereotype.Component;

@Component
public class RatingFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, MaidSearchRequest request) {
        return request.getMinRating() == null || maid.getRating() == null || maid.getRating() >= request.getMinRating();
    }
}
