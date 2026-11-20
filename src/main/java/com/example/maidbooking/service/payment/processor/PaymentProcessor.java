package com.example.maidbooking.service.payment.processor;

import com.example.maidbooking.enums.PaymentMethod;
import com.example.maidbooking.service.payment.gateway.GatewayResponse;

import java.math.BigDecimal;

public interface PaymentProcessor {

    PaymentMethod method();

    GatewayResponse pay(BigDecimal amount, String instrument);
}
