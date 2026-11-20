package com.example.maidbooking.service.cancellation;

import com.example.maidbooking.domain.Booking;
import com.example.maidbooking.domain.Occurrence;
import com.example.maidbooking.domain.Refund;
import com.example.maidbooking.dto.response.CancellationResponse;
import com.example.maidbooking.exception.InvalidStateException;
import com.example.maidbooking.service.BookingService;
import com.example.maidbooking.service.MaidLocks;
import com.example.maidbooking.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationService {

    private static final BigDecimal NO_REFUND = BigDecimal.ZERO.setScale(2);

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationPolicy cancellationPolicy;
    private final MaidLocks maidLocks;
    private final Clock clock;

    public CancellationResponse cancelOccurrence(String bookingId, String occurrenceId) {
        String maidId = bookingService.getBooking(bookingId).getMaidId();

        List<Refund> refunds = maidLocks.withLock(maidId, () -> {
            Booking booking = bookingService.getBooking(bookingId);
            Occurrence occurrence = booking.findOccurrence(occurrenceId);
            return List.of(cancel(booking, occurrence, LocalDateTime.now(clock)));
        });
        return new CancellationResponse(bookingId, refunds);
    }

    public CancellationResponse cancelBooking(String bookingId) {
        String maidId = bookingService.getBooking(bookingId).getMaidId();

        List<Refund> refunds = maidLocks.withLock(maidId, () -> {
            Booking booking = bookingService.getBooking(bookingId);
            LocalDateTime now = LocalDateTime.now(clock);
            List<Occurrence> upcoming = booking.upcomingOccurrences(now);
            if (upcoming.isEmpty()) {
                throw new InvalidStateException("Booking " + bookingId + " has no upcoming occurrences to cancel");
            }
            log.info("Cancelling {} upcoming occurrence(s) of booking {}", upcoming.size(), bookingId);
            return upcoming.stream().map(occurrence -> cancel(booking, occurrence, now)).toList();
        });
        return new CancellationResponse(bookingId, refunds);
    }

    private Refund cancel(Booking booking, Occurrence occurrence, LocalDateTime now) {
        occurrence.ensureCancellable(now);

        BigDecimal refundAmount = occurrence.isPaid() ? cancellationPolicy.refundAmount(occurrence, now) : NO_REFUND;
        if (refundAmount.signum() > 0) {
            paymentService.refund(occurrence.getId(), refundAmount);
        }
        occurrence.cancel(now);
        bookingService.save(booking);
        log.info("Occurrence {} cancelled with refund {}", occurrence.getId(), refundAmount);
        return new Refund(occurrence.getId(), refundAmount);
    }
}
