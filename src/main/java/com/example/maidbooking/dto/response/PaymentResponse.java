package com.example.maidbooking.dto.response;

import com.example.maidbooking.domain.Payment;
import com.example.maidbooking.enums.PaymentMethod;
import com.example.maidbooking.enums.PaymentStatus;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PaymentResponse {

    private final String id;
    private final String bookingId;
    private final String occurrenceId;
    private final BigDecimal amount;
    private final PaymentMethod method;
    private final PaymentStatus status;
    private final String gatewayReference;
    private final BigDecimal refundedAmount;

    private PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.bookingId = payment.getBookingId();
        this.occurrenceId = payment.getOccurrenceId();
        this.amount = payment.getAmount();
        this.method = payment.getMethod();
        this.status = payment.getStatus();
        this.gatewayReference = payment.getGatewayReference();
        this.refundedAmount = payment.getRefundedAmount();
    }

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment);
    }
}
