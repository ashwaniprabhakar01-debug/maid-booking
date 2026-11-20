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
public class CardPaymentProcessor implements PaymentProcessor {

    private static final Pattern CARD_NUMBER = Pattern.compile("\\d{12,19}");

    private final PaymentGateway gateway;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    public GatewayResponse pay(BigDecimal amount, String instrument) {
        if (instrument == null || !CARD_NUMBER.matcher(instrument).matches()) {
            throw new IllegalArgumentException("Invalid card number");
        }
        return gateway.charge(method(), instrument, amount);
    }
}
