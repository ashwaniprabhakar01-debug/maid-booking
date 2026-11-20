package com.example.maidbooking.service.search;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.dto.request.MaidSearchRequest;

public interface MaidFilter {

    boolean matches(Maid maid, MaidSearchRequest request);
}
