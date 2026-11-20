package com.example.maidbooking.repository.inmemory;

import com.example.maidbooking.domain.Payment;
import com.example.maidbooking.repository.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<String, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        payments.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public List<Payment> findByOccurrenceId(String occurrenceId) {
        return payments.values().stream()
                .filter(payment -> payment.getOccurrenceId().equals(occurrenceId))
                .toList();
    }
}
