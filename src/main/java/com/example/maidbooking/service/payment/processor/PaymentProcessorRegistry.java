package com.example.maidbooking.service.payment.processor;

import com.example.maidbooking.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentProcessorRegistry {

    private final Map<PaymentMethod, PaymentProcessor> processors = new EnumMap<>(PaymentMethod.class);

    public PaymentProcessorRegistry(List<PaymentProcessor> processors) {
        processors.forEach(processor -> this.processors.put(processor.method(), processor));
    }

    public PaymentProcessor get(PaymentMethod method) {
        PaymentProcessor processor = processors.get(method);
        if (processor == null) {
            throw new IllegalArgumentException("Unsupported payment method: " + method);
        }
        return processor;
    }
}
