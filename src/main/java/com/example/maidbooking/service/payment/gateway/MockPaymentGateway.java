package com.example.maidbooking.service.payment.gateway;

import com.example.maidbooking.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount) {
        return GatewayResponse.success("txn_" + UUID.randomUUID());
    }

    @Override
    public GatewayResponse refund(String reference, BigDecimal amount) {
        return GatewayResponse.success("rfnd_" + UUID.randomUUID());
    }
}
