package com.example.maidbooking.service.payment.gateway;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GatewayResponse {

    private final boolean success;
    private final String reference;

    public static GatewayResponse success(String reference) {
        return new GatewayResponse(true, reference);
    }

    public static GatewayResponse failure() {
        return new GatewayResponse(false, null);
    }
}
