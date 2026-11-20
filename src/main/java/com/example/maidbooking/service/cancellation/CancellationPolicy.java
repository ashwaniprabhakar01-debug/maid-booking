package com.example.maidbooking.service.cancellation;

import com.example.maidbooking.domain.Occurrence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CancellationPolicy {

    BigDecimal refundAmount(Occurrence occurrence, LocalDateTime cancelledAt);
}
