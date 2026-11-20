package com.example.maidbooking.service.search;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.dto.request.MaidSearchRequest;
import org.springframework.stereotype.Component;

@Component
public class ServiceFilter implements MaidFilter {

    @Override
    public boolean matches(Maid maid, MaidSearchRequest request) {
        return request.getServices() == null || maid.getServices().containsAll(request.getServices());
    }
}
