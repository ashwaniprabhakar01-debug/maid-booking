package com.example.maidbooking.service.payment.processor;

import com.example.maidbooking.enums.PaymentMethod;
import com.example.maidbooking.service.payment.gateway.GatewayResponse;
import com.example.maidbooking.service.payment.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class WalletPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WALLET;
    }

    @Override
    public GatewayResponse pay(BigDecimal amount, String instrument) {
        if (instrument == null || instrument.isBlank()) {
            throw new IllegalArgumentException("Wallet id is required");
        }
        return gateway.charge(method(), instrument, amount);
    }
}
