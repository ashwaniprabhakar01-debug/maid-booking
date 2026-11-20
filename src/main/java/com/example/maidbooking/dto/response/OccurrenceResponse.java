package com.example.maidbooking.dto.response;

import com.example.maidbooking.domain.Occurrence;
import com.example.maidbooking.enums.OccurrenceStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class OccurrenceResponse {

    private final String id;
    private final LocalDateTime start;
    private final LocalDateTime end;
    private final BigDecimal amount;
    private final OccurrenceStatus status;

    private OccurrenceResponse(Occurrence occurrence) {
        this.id = occurrence.getId();
        this.start = occurrence.getSlot().getStart();
        this.end = occurrence.getSlot().getEnd();
        this.amount = occurrence.getAmount();
        this.status = occurrence.getStatus();
    }

    public static OccurrenceResponse from(Occurrence occurrence) {
        return new OccurrenceResponse(occurrence);
    }
}
