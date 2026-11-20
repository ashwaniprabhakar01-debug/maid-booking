package com.example.maidbooking.service.cancellation;

import com.example.maidbooking.domain.Booking;
import com.example.maidbooking.domain.Occurrence;
import com.example.maidbooking.domain.Payment;
import com.example.maidbooking.domain.Refund;
import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.PaymentRequest;
import com.example.maidbooking.enums.BookingType;
import com.example.maidbooking.enums.OccurrenceStatus;
import com.example.maidbooking.enums.PaymentMethod;
import com.example.maidbooking.enums.PaymentStatus;
import com.example.maidbooking.enums.ServiceType;
import com.example.maidbooking.exception.InvalidStateException;
import com.example.maidbooking.exception.PaymentFailedException;
import com.example.maidbooking.repository.inmemory.InMemoryMaidRepository;
import com.example.maidbooking.repository.inmemory.InMemoryPaymentRepository;
import com.example.maidbooking.repository.inmemory.StatusRecordingBookingRepository;
import com.example.maidbooking.service.AvailabilityChecker;
import com.example.maidbooking.service.BookingService;
import com.example.maidbooking.service.MaidLocks;
import com.example.maidbooking.service.MaidService;
import com.example.maidbooking.service.payment.PaymentService;
import com.example.maidbooking.service.payment.gateway.GatewayResponse;
import com.example.maidbooking.service.payment.gateway.PaymentGateway;
import com.example.maidbooking.service.payment.processor.PaymentProcessorRegistry;
import com.example.maidbooking.service.payment.processor.WalletPaymentProcessor;
import com.example.maidbooking.service.slot.SlotGeneratorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancellationServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 10, 0);

    private final StatusRecordingBookingRepository bookingRepository = new StatusRecordingBookingRepository();
    private final InMemoryPaymentRepository paymentRepository = new InMemoryPaymentRepository();
    private final RefundDecliningGateway gateway = new RefundDecliningGateway();
    private PaymentService paymentService;
    private CancellationService cancellationService;

    private final Occurrence past = occurrence("past", NOW.minusDays(1));
    private final Occurrence tomorrow = occurrence("tomorrow", NOW.plusHours(20));
    private final Occurrence nextWeek = occurrence("nextWeek", NOW.plusDays(7));

    @BeforeEach
    void setUp() {
        MaidLocks maidLocks = new MaidLocks();
        BookingService bookingService = new BookingService(new MaidService(new InMemoryMaidRepository()),
                bookingRepository, new SlotGeneratorRegistry(List.of()),
                new AvailabilityChecker(bookingRepository), maidLocks);
        paymentService = new PaymentService(bookingService, paymentRepository,
                new PaymentProcessorRegistry(List.of(new WalletPaymentProcessor(gateway))), gateway, maidLocks);
        cancellationService = new CancellationService(bookingService, paymentService,
                new FullRefundCancellationPolicy(), maidLocks,
                Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

        bookingRepository.save(new Booking("b1", "c1", "m1", ServiceType.CLEANING, BookingType.RECURRING,
                List.of(past, tomorrow, nextWeek)));
    }

    @Test
    void cancellingPaidOccurrenceRefundsFullAmount() {
        pay("nextWeek");

        List<Refund> refunds = cancellationService.cancelOccurrence("b1", "nextWeek").getRefunds();

        assertEquals(new BigDecimal("500.00"), refunds.get(0).getAmount());
        assertEquals(OccurrenceStatus.CANCELLED, nextWeek.getStatus());
        Payment payment = paymentRepository.findByOccurrenceId("nextWeek").get(0);
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(new BigDecimal("500.00"), payment.getRefundedAmount());
    }

    @Test
    void cancellingUnpaidOccurrenceRefundsNothing() {
        List<Refund> refunds = cancellationService.cancelOccurrence("b1", "nextWeek").getRefunds();

        assertEquals(new BigDecimal("0.00"), refunds.get(0).getAmount());
        assertEquals(OccurrenceStatus.CANCELLED, nextWeek.getStatus());
    }

    @Test
    void cancellingOneOccurrenceLeavesTheRestOfTheSeries() {
        cancellationService.cancelOccurrence("b1", "tomorrow");

        assertEquals(OccurrenceStatus.PENDING_PAYMENT, nextWeek.getStatus());
    }

    @Test
    void cancellingSeriesCancelsOnlyUpcomingOccurrences() {
        pay("tomorrow");

        List<Refund> refunds = cancellationService.cancelBooking("b1").getRefunds();

        assertEquals(List.of("tomorrow", "nextWeek"), refunds.stream().map(Refund::getOccurrenceId).toList());
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, past.getStatus());
        assertEquals(OccurrenceStatus.CANCELLED, tomorrow.getStatus());
        assertEquals(OccurrenceStatus.CANCELLED, nextWeek.getStatus());
    }

    @Test
    void cancelledOccurrencesAreSavedToTheRepository() {
        cancellationService.cancelBooking("b1");

        assertEquals(OccurrenceStatus.CANCELLED, bookingRepository.savedStatus("tomorrow"));
        assertEquals(OccurrenceStatus.CANCELLED, bookingRepository.savedStatus("nextWeek"));
        assertEquals(OccurrenceStatus.PENDING_PAYMENT, bookingRepository.savedStatus("past"));
    }

    @Test
    void cannotCancelSeriesTwice() {
        cancellationService.cancelBooking("b1");

        assertThrows(InvalidStateException.class, () -> cancellationService.cancelBooking("b1"));
    }

    @Test
    void cannotCancelOccurrenceThatAlreadyStarted() {
        assertThrows(InvalidStateException.class, () -> cancellationService.cancelOccurrence("b1", "past"));
    }

    @Test
    void failedRefundLeavesOccurrenceActiveSoCancellationCanBeRetried() {
        pay("nextWeek");
        gateway.declineRefunds = true;

        assertThrows(PaymentFailedException.class, () -> cancellationService.cancelOccurrence("b1", "nextWeek"));
        assertEquals(OccurrenceStatus.CONFIRMED, nextWeek.getStatus());
        assertEquals(PaymentStatus.SUCCESS, paymentRepository.findByOccurrenceId("nextWeek").get(0).getStatus());

        gateway.declineRefunds = false;
        List<Refund> refunds = cancellationService.cancelOccurrence("b1", "nextWeek").getRefunds();

        assertEquals(new BigDecimal("500.00"), refunds.get(0).getAmount());
        assertEquals(OccurrenceStatus.CANCELLED, nextWeek.getStatus());
    }

    private void pay(String occurrenceId) {
        PaymentRequest request = new PaymentRequest();
        request.setMethod(PaymentMethod.WALLET);
        request.setInstrument("wallet-1");
        paymentService.pay("b1", occurrenceId, request);
    }

    private static Occurrence occurrence(String id, LocalDateTime start) {
        return new Occurrence(id, TimeSlot.of(start, Duration.ofHours(2)), new BigDecimal("500.00"));
    }

    private static class RefundDecliningGateway implements PaymentGateway {

        private boolean declineRefunds;

        @Override
        public GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount) {
            return GatewayResponse.success("txn-" + instrument);
        }

        @Override
        public GatewayResponse refund(String reference, BigDecimal amount) {
            return declineRefunds ? GatewayResponse.failure() : GatewayResponse.success("refund-" + reference);
        }
    }
}
