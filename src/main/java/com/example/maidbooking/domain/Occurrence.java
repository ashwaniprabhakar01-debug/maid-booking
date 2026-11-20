package com.example.maidbooking.domain;

import com.example.maidbooking.enums.OccurrenceStatus;
import com.example.maidbooking.exception.InvalidStateException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class Occurrence {

    private final String id;
    private final TimeSlot slot;
    private final BigDecimal amount;
    private volatile OccurrenceStatus status = OccurrenceStatus.PENDING_PAYMENT;

    public void confirm() {
        if (status != OccurrenceStatus.PENDING_PAYMENT) {
            throw new InvalidStateException("Occurrence " + id + " cannot be confirmed in state " + status);
        }
        status = OccurrenceStatus.CONFIRMED;
    }

    public void cancel(LocalDateTime now) {
        ensureCancellable(now);
        status = OccurrenceStatus.CANCELLED;
    }

    public void ensureCancellable(LocalDateTime now) {
        if (status == OccurrenceStatus.CANCELLED) {
            throw new InvalidStateException("Occurrence " + id + " is already cancelled");
        }
        if (!startsAfter(now)) {
            throw new InvalidStateException("Occurrence " + id + " has already started");
        }
    }

    public boolean startsAfter(LocalDateTime time) {
        return slot.getStart().isAfter(time);
    }

    public void ensurePayable() {
        if (status != OccurrenceStatus.PENDING_PAYMENT) {
            throw new InvalidStateException("Occurrence " + id + " cannot be paid in state " + status);
        }
    }

    public boolean isActive() {
        return status != OccurrenceStatus.CANCELLED;
    }

    public boolean isPaid() {
        return status == OccurrenceStatus.CONFIRMED;
    }
}
