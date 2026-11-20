package com.example.maidbooking.dto.response;

import com.example.maidbooking.domain.Refund;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class CancellationResponse {

    private final String bookingId;
    private final List<Refund> refunds;
    private final BigDecimal totalRefund;

    public CancellationResponse(String bookingId, List<Refund> refunds) {
        this.bookingId = bookingId;
        this.refunds = refunds;
        this.totalRefund = refunds.stream().map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
