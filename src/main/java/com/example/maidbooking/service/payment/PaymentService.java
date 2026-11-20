package com.example.maidbooking.service.payment;

import com.example.maidbooking.domain.Booking;
import com.example.maidbooking.domain.Occurrence;
import com.example.maidbooking.domain.Payment;
import com.example.maidbooking.dto.request.PaymentRequest;
import com.example.maidbooking.dto.response.PaymentResponse;
import com.example.maidbooking.exception.InvalidStateException;
import com.example.maidbooking.exception.PaymentFailedException;
import com.example.maidbooking.repository.PaymentRepository;
import com.example.maidbooking.service.BookingService;
import com.example.maidbooking.service.MaidLocks;
import com.example.maidbooking.service.payment.gateway.GatewayResponse;
import com.example.maidbooking.service.payment.gateway.PaymentGateway;
import com.example.maidbooking.service.payment.processor.PaymentProcessorRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingService bookingService;
    private final PaymentRepository paymentRepository;
    private final PaymentProcessorRegistry processorRegistry;
    private final PaymentGateway paymentGateway;
    private final MaidLocks maidLocks;

    public PaymentResponse pay(String bookingId, String occurrenceId, PaymentRequest request) {
        String maidId = bookingService.getBooking(bookingId).getMaidId();

        return maidLocks.withLock(maidId, () -> {
            Booking booking = bookingService.getBooking(bookingId);
            Occurrence occurrence = booking.findOccurrence(occurrenceId);
            occurrence.ensurePayable();
            Payment payment = new Payment(UUID.randomUUID().toString(), bookingId, occurrenceId,
                    occurrence.getAmount(), request.getMethod());

            GatewayResponse response = processorRegistry.get(request.getMethod())
                    .pay(occurrence.getAmount(), request.getInstrument());

            if (!response.isSuccess()) {
                log.warn("Payment {} declined for occurrence {} via {}", payment.getId(), occurrenceId, request.getMethod());
                payment.markFailed();
                paymentRepository.save(payment);
                throw new PaymentFailedException("Payment declined for occurrence " + occurrenceId);
            }
            payment.markSuccess(response.getReference());
            occurrence.confirm();
            bookingService.save(booking);
            log.info("Payment {} of {} via {} succeeded for occurrence {}",
                    payment.getId(), payment.getAmount(), request.getMethod(), occurrenceId);
            return PaymentResponse.from(paymentRepository.save(payment));
        });
    }

    public void refund(String occurrenceId, BigDecimal amount) {
        Payment payment = paymentRepository.findByOccurrenceId(occurrenceId).stream()
                .filter(Payment::isSuccessful)
                .findFirst()
                .orElseThrow(() -> new InvalidStateException("No successful payment for occurrence " + occurrenceId));

        GatewayResponse response = paymentGateway.refund(payment.getGatewayReference(), amount);
        if (!response.isSuccess()) {
            log.warn("Refund of {} declined for payment {} (occurrence {})", amount, payment.getId(), occurrenceId);
            throw new PaymentFailedException("Refund failed for occurrence " + occurrenceId);
        }
        payment.markRefunded(amount);
        paymentRepository.save(payment);
        log.info("Refunded {} for payment {} (occurrence {})", amount, payment.getId(), occurrenceId);
    }
}
