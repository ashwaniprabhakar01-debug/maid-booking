package com.example.maidbooking.service.search;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.dto.request.MaidSearchRequest;
import org.springframework.stereotype.Component;

@Component
public class LocalityFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, MaidSearchRequest request) {
        return request.getLocality() == null
                || request.getLocality().isBlank()
                || maid.getLocality().equalsIgnoreCase(request.getLocality().trim());
    }
}
