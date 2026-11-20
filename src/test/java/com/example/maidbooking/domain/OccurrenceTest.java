package com.example.maidbooking.domain;

import com.example.maidbooking.enums.OccurrenceStatus;
import com.example.maidbooking.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OccurrenceTest {

    private static final LocalDateTime BEFORE_START = LocalDateTime.of(2026, 11, 30, 10, 0);

    private final Occurrence occurrence = new Occurrence("o1",
            new TimeSlot(LocalDateTime.of(2026, 12, 1, 9, 0), LocalDateTime.of(2026, 12, 1, 11, 0)),
            BigDecimal.valueOf(500));

    @Test
    void confirmMovesPendingToConfirmed() {
        occurrence.confirm();
        assertEquals(OccurrenceStatus.CONFIRMED, occurrence.getStatus());
    }

    @Test
    void cannotConfirmTwice() {
        occurrence.confirm();
        assertThrows(InvalidStateException.class, occurrence::confirm);
    }

    @Test
    void cannotCancelTwice() {
        occurrence.cancel(BEFORE_START);
        assertThrows(InvalidStateException.class, () -> occurrence.cancel(BEFORE_START));
    }
}
