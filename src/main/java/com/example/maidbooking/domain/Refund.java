package com.example.maidbooking.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class Refund {

    private final String occurrenceId;
    private final BigDecimal amount;
}
