package com.example.maidbooking.service.payment.processor;

import com.example.maidbooking.enums.PaymentMethod;
import com.example.maidbooking.service.payment.gateway.GatewayResponse;
import com.example.maidbooking.service.payment.gateway.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class UpiPaymentProcessor implements PaymentProcessor {

    private static final Pattern UPI_ID = Pattern.compile("[\\w.-]+@[a-zA-Z]+");

    private final PaymentGateway gateway;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    public GatewayResponse pay(BigDecimal amount, String instrument) {
        if (instrument == null || !UPI_ID.matcher(instrument).matches()) {
            throw new IllegalArgumentException("Invalid UPI id");
        }
        return gateway.charge(method(), instrument, amount);
    }
}
