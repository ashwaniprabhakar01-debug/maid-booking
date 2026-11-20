package com.example.maidbooking.service.payment;

import com.example.maidbooking.domain.Booking;
import com.example.maidbooking.domain.Occurrence;
import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.PaymentRequest;
import com.example.maidbooking.dto.response.PaymentResponse;
import com.example.maidbooking.enums.BookingType;
import com.example.maidbooking.enums.OccurrenceStatus;
import com.example.maidbooking.enums.PaymentMethod;
import com.example.maidbooking.enums.PaymentStatus;
import com.example.maidbooking.enums.ServiceType;
import com.example.maidbooking.exception.InvalidStateException;
import com.example.maidbooking.exception.PaymentFailedException;
import com.example.maidbooking.exception.ResourceNotFoundException;
import com.example.maidbooking.repository.inmemory.InMemoryMaidRepository;
import com.example.maidbooking.repository.inmemory.InMemoryPaymentRepository;
import com.example.maidbooking.repository.inmemory.StatusRecordingBookingRepository;
import com.example.maidbooking.service.AvailabilityChecker;
import com.example.maidbooking.service.BookingService;
import com.example.maidbooking.service.MaidLocks;
import com.example.maidbooking.service.MaidService;
import com.example.maidbooking.service.payment.gateway.GatewayResponse;
import com.example.maidbooking.service.payment.gateway.PaymentGateway;
import com.example.maidbooking.service.payment.processor.CardPaymentProcessor;
import com.example.maidbooking.service.payment.processor.PaymentProcessorRegistry;
import com.example.maidbooking.service.payment.processor.UpiPaymentProcessor;
import com.example.maidbooking.service.payment.processor.WalletPaymentProcessor;
import com.example.maidbooking.service.slot.SlotGeneratorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentServiceTest {

    private final StatusRecordingBookingRepository bookingRepository = new StatusRecordingBookingRepository();
    private final InMemoryPaymentRepository paymentRepository = new InMemoryPaymentRepository();
    private final FakeGateway gateway = new FakeGateway();
    private PaymentService paymentService;

    private final Occurrence first = occurrence("o1", 1);
    private final Occurrence second = occurrence("o2", 8);

    @BeforeEach
    void setUp() {
        MaidLocks maidLocks = new MaidLocks();
        BookingService bookingService = new BookingService(new MaidService(new InMemoryMaidRepository()),
                bookingRepository, new SlotGeneratorRegistry(List.of()),
                new AvailabilityChecker(bookingRepository), maidLocks);
        PaymentProcessorRegistry processors = new PaymentProcessorRegistry(List.of(
                new CardPaymentProcessor(gateway),
                new UpiPaymentProcessor(gateway),
                new WalletPaymentProcessor(gateway)));
        paymentService = new PaymentService(bookingService, paymentRepository, processors, gateway, maidLocks);

        bookingRepository.save(new Booking("b1", "c1", "m1", ServiceType.CLEANING, BookingType.RECURRING,
                List.of(first, second)));
    }

    @Test
    void successfulPaymentConfirmsOnlyThatOccurrence() {
        PaymentResponse payment = paymentService.pay("b1", "o1", request(PaymentMethod.UPI, "sunita@okaxis"));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(new BigDecimal("500"), payment.getAmount());
        assertEquals(OccurrenceStatus.CONFIRMED, first.getStatus());
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, second.getStatus());
    }

    @Test
    void confirmedOccurrenceIsSavedToTheRepository() {
        paymentService.pay("b1", "o1", request(PaymentMethod.UPI, "sunita@okaxis"));

        assertEquals(OccurrenceStatus.CONFIRMED, bookingRepository.savedStatus("o1"));
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, bookingRepository.savedStatus("o2"));
    }

    @Test
    void cannotPayTheSameOccurrenceTwice() {
        paymentService.pay("b1", "o1", request(PaymentMethod.CARD, "4111111111111111"));

        assertThrows(InvalidStateException.class,
                () -> paymentService.pay("b1", "o1", request(PaymentMethod.WALLET, "w-1")));
        assertEquals(1, gateway.charges);
    }

    @Test
    void cannotPayCancelledOccurrence() {
        first.cancel(LocalDateTime.of(2026, 11, 30, 10, 0));

        assertThrows(InvalidStateException.class,
                () -> paymentService.pay("b1", "o1", request(PaymentMethod.WALLET, "w-1")));
        assertEquals(0, gateway.charges);
    }

    @Test
    void declinedPaymentIsRecordedAndOccurrenceStaysUnpaid() {
        gateway.decline = true;

        assertThrows(PaymentFailedException.class,
                () -> paymentService.pay("b1", "o1", request(PaymentMethod.WALLET, "w-1")));

        assertEquals(OccurrenceStatus.PENDING_PAYMENT, first.getStatus());
        assertEquals(PaymentStatus.FAILED, paymentRepository.findByOccurrenceId("o1").get(0).getStatus());
    }

    @Test
    void invalidInstrumentIsRejectedBeforeCharging() {
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay("b1", "o1", request(PaymentMethod.UPI, "not-a-upi-id")));
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.pay("b1", "o1", request(PaymentMethod.CARD, "1234")));
        assertEquals(0, gateway.charges);
    }

    @Test
    void concurrentPaymentsForSameOccurrenceChargeOnlyOnce() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            start.await();
            try {
                paymentService.pay("b1", "o1", request(PaymentMethod.WALLET, "w-1"));
                return true;
            } catch (InvalidStateException e) {
                return false;
            }
        };
        Future<Boolean> firstAttempt = executor.submit(attempt);
        Future<Boolean> secondAttempt = executor.submit(attempt);
        start.countDown();

        int successes = (firstAttempt.get() ? 1 : 0) + (secondAttempt.get() ? 1 : 0);
        executor.shutdown();

        assertEquals(1, successes);
        assertEquals(1, gateway.charges);
    }

    @Test
    void unknownBookingThrowsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.pay("missing", "o1", request(PaymentMethod.WALLET, "w-1")));
    }

    private static Occurrence occurrence(String id, int day) {
        return new Occurrence(id, TimeSlot.of(LocalDateTime.of(2026, 12, day, 9, 0), Duration.ofHours(2)),
                new BigDecimal("500"));
    }

    private static PaymentRequest request(PaymentMethod method, String instrument) {
        PaymentRequest request = new PaymentRequest();
        request.setMethod(method);
        request.setInstrument(instrument);
        return request;
    }

    private static class FakeGateway implements PaymentGateway {

        private boolean decline;
        private int charges;

        @Override
        public GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount) {
            charges++;
            return decline ? GatewayResponse.failure() : GatewayResponse.success("txn-" + charges);
        }

        @Override
        public GatewayResponse refund(String reference, BigDecimal amount) {
            return GatewayResponse.success("refund-" + reference);
        }
    }
}
