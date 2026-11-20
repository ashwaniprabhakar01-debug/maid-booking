package com.example.maidbooking.repository;

import com.example.maidbooking.domain.Payment;

import java.util.List;

public interface PaymentRepository {

    Payment save(Payment payment);

    List<Payment> findByOccurrenceId(String occurrenceId);
}
