package com.example.maidbooking.domain;

import com.example.maidbooking.enums.PaymentMethod;
import com.example.maidbooking.enums.PaymentStatus;
import com.example.maidbooking.exception.InvalidStateException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class Payment {

    private final String id;
    private final String bookingId;
    private final String occurrenceId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private PaymentStatus status = PaymentStatus.PENDING;
    private String gatewayReference;
    private BigDecimal refundedAmount = BigDecimal.ZERO.setScale(2);

    public void markSuccess(String gatewayReference) {
        ensurePending();
        this.gatewayReference = gatewayReference;
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed() {
        ensurePending();
        this.status = PaymentStatus.FAILED;
    }

    public void markRefunded(BigDecimal refundAmount) {
        if (status != PaymentStatus.SUCCESS) {
            throw new InvalidStateException("Payment " + id + " cannot be refunded in state " + status);
        }
        if (refundAmount.signum() <= 0 || refundAmount.compareTo(amount) > 0) {
            throw new IllegalArgumentException("Refund amount must be between 0 and " + amount);
        }
        this.refundedAmount = refundAmount;
        this.status = PaymentStatus.REFUNDED;
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }

    private void ensurePending() {
        if (status != PaymentStatus.PENDING) {
            throw new InvalidStateException("Payment " + id + " is already " + status);
        }
    }
}
