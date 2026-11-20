package com.example.maidbooking.service.payment.gateway;

import com.example.maidbooking.enums.PaymentMethod;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount);

    GatewayResponse refund(String reference, BigDecimal amount);
}
