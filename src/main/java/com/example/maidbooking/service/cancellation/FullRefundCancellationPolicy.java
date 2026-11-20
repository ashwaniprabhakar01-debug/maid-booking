package com.example.maidbooking.service.cancellation;

import com.example.maidbooking.domain.Occurrence;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class FullRefundCancellationPolicy implements CancellationPolicy {

    @Override
    public BigDecimal refundAmount(Occurrence occurrence, LocalDateTime cancelledAt) {
        return occurrence.getAmount();
    }
}
