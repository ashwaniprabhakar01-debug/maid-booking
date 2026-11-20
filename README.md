# Maid Booking Service

Backend for a home-services platform: onboard maids, discover available maids, book them (instant, scheduled or recurring), pay per visit and cancel with refunds.

Java 17, Spring Boot 3.5, Lombok, in-memory storage.

## Build and run

Requirements: JDK 17 installed. The code compiles with a Java 17 toolchain, which Gradle finds automatically. Gradle itself (8.14, through the wrapper) can be started with any JDK from 17 to 24, so a newer JDK as the default `java` is fine as long as JDK 17 is also installed.

```bash
./gradlew test       # run all tests
./gradlew bootRun    # starts on http://localhost:8081
```

Use `PORT=9090 ./gradlew bootRun` to change the port.

The code uses Lombok for getters and setters. Gradle handles it automatically; in an IDE, enable annotation processing (IntelliJ: Settings → Build → Compiler → Annotation Processors).

## API

| Operation | Endpoint |
|---|---|
| Add a maid | `POST /maids` |
| Search maids | `GET /maids/search` |
| Book a maid | `POST /bookings` |
| Pay for an occurrence | `POST /bookings/{bookingId}/occurrences/{occurrenceId}/payments` |
| Cancel one occurrence | `POST /bookings/{bookingId}/occurrences/{occurrenceId}/cancel` |
| Cancel a whole booking | `POST /bookings/{bookingId}/cancel` |

### Add a maid

```bash
curl -X POST localhost:8081/maids -H 'Content-Type: application/json' -d '{
  "name": "Sunita",
  "gender": "FEMALE",
  "locality": "Koramangala",
  "services": ["CLEANING", "COOKING"],
  "pricePerHour": 250,
  "rating": 4.5,
  "availability": [
    {"dayOfWeek": "TUESDAY", "startTime": "08:00", "endTime": "18:00"},
    {"dayOfWeek": "THURSDAY", "startTime": "08:00", "endTime": "18:00"}
  ]
}'
```

### Search maids

All parameters are optional and combine with AND.

```bash
curl 'localhost:8081/maids/search?locality=Koramangala&services=CLEANING&services=COOKING&startTime=2026-12-01T09:00&durationMinutes=120&minRating=4&maxPrice=300&gender=FEMALE'
```

`services` can be repeated; a maid must offer every listed service. `startTime` and `durationMinutes` must be sent together. When they are, only maids who work at that time and are not already booked are returned.

### Book a maid

Common fields: `customerId`, `maidId`, `serviceType`, `bookingType`, `durationMinutes`.

```bash
# Instant: starts in 30 minutes (time for the maid to arrive)
{"customerId":"c1","maidId":"<id>","serviceType":"CLEANING","bookingType":"INSTANT","durationMinutes":60}

# Scheduled: one future slot
{"customerId":"c1","maidId":"<id>","serviceType":"CLEANING","bookingType":"SCHEDULED","durationMinutes":120,
 "startTime":"2026-12-01T09:00"}

# Recurring: every chosen weekday between two dates
{"customerId":"c1","maidId":"<id>","serviceType":"CLEANING","bookingType":"RECURRING","durationMinutes":60,
 "daysOfWeek":["TUESDAY","THURSDAY"],"timeOfDay":"09:00","startDate":"2026-12-01","endDate":"2026-12-31"}
```

The response lists the booking's occurrences. Each occurrence has its own id, time slot, amount and status.

### Pay and cancel

```bash
curl -X POST localhost:8081/bookings/<bookingId>/occurrences/<occurrenceId>/payments \
  -H 'Content-Type: application/json' -d '{"method":"UPI","instrument":"customer@okaxis"}'

curl -X POST localhost:8081/bookings/<bookingId>/occurrences/<occurrenceId>/cancel
curl -X POST localhost:8081/bookings/<bookingId>/cancel
```

`method` is `CARD`, `UPI` or `WALLET`. `instrument` is the card number, UPI id or wallet id.

### Errors

| Status | When |
|---|---|
| 400 | Invalid input |
| 402 | Payment or refund declined by the gateway |
| 404 | Unknown maid, booking or occurrence |
| 409 | Slot unavailable, double booking, or an invalid state change (paying twice, cancelling twice, cancelling a started occurrence) |

## Design

### Packages

```
domain/                entities and value objects with the business rules (no Spring)
enums/                 service types, gender, booking types, payment methods and statuses
repository/            repository interfaces, with in-memory implementations under inmemory/
service/               maid onboarding, booking, availability, per-maid locks
service/slot/          booking types (slot generators)
service/search/        search filters
service/payment/       payment service, processors per method, gateway abstraction
service/cancellation/  cancellation service and refund policy
controller/            REST endpoints
dto/request/           request objects with input validation
dto/response/          response objects returned by the API
exception/             exceptions and the global error handler
config/                Clock bean
```

### Domain model

- **Maid**: profile, services, hourly price, rating and weekly availability windows.
- **AvailabilityWindow**: a day of the week with start and end times.
- **TimeSlot**: an immutable start/end value object. Intervals are half-open, so 9–10 and 10–11 do not overlap.
- **Booking**: one customer, one maid, one service and one booking type, holding one or more occurrences.
- **Occurrence**: one visit with its own slot, amount and status (`PENDING_PAYMENT → CONFIRMED`; either can move to `CANCELLED`).
- **Payment**: one payment attempt for one occurrence (`PENDING → SUCCESS | FAILED`, `SUCCESS → REFUNDED`).

An occurrence is the unit that gets booked, paid and cancelled. Instant and scheduled bookings have one occurrence; recurring bookings have many. This one choice covers paying per occurrence and cancelling a single occurrence or the whole series without any special cases.

### Extension points

Each variable part sits behind an interface. Adding a new kind means writing one new class (plus an enum value where one applies). Services and registries do not change; if the new kind needs new input, its fields are added to the request DTO.

| Requirement | Interface | Implementations |
|---|---|---|
| Booking types | `SlotGenerator` | `InstantSlotGenerator`, `ScheduledSlotGenerator`, `RecurringSlotGenerator` |
| Search filters | `MaidFilter` | locality, services, rating, price, gender, availability |
| Payment methods | `PaymentProcessor` | `CardPaymentProcessor`, `UpiPaymentProcessor`, `WalletPaymentProcessor` |
| Payment provider | `PaymentGateway` | `MockPaymentGateway` |
| Refund rule | `CancellationPolicy` | `FullRefundCancellationPolicy` |
| Storage | `MaidRepository`, `BookingRepository`, `PaymentRepository` | in-memory implementations |

Booking types and payment methods are looked up through enum-keyed registries (`SlotGeneratorRegistry`, `PaymentProcessorRegistry`). Spring injects every search filter into `MaidSearchService` as a list.

### Availability and double booking

`AvailabilityChecker` is the single place that decides whether a maid is free: the slot must fall inside one of the maid's windows and must not overlap any active occurrence. Booking and search both use it. Cancelled occurrences are ignored, so cancelling frees the slot straight away.

A recurring booking is all or nothing: if any occurrence clashes, nothing is saved.

### Concurrency

`MaidLocks` keeps one lock per maid. Booking (check then save), payment and cancellation for a maid run under that lock, so:

- two customers cannot book the same maid for overlapping slots at the same time;
- an occurrence cannot be paid twice, or paid and cancelled at the same time;
- bookings for different maids do not block each other.

Inside the lock, services re-read the booking from the repository and save it after every state change (payment confirmed, occurrence cancelled). Nothing relies on the in-memory repository returning the same object, so it can be swapped for a database without losing updates.

### Logging

Services log business events with SLF4J (Lombok `@Slf4j`). Domain classes do not log.

| Level | Events |
|---|---|
| INFO | maid added, booking created, booking rejected (slot unavailable), payment succeeded, refund done, occurrence or series cancelled |
| WARN | payment or refund declined by the gateway |
| DEBUG | number of search results |

Logs carry ids, amounts and payment methods only. Card numbers, UPI ids, wallet ids and maid names are never logged. To see search logs, start with `--logging.level.com.example.maidbooking=DEBUG`, for example `./gradlew bootRun --args='--logging.level.com.example.maidbooking=DEBUG'`.

## Assumptions

- All times are in a single timezone (the server's). `LocalDateTime` is used because bookings are wall-clock times ("9am every Tuesday").
- Availability windows repeat every week and do not cross midnight. Windows on the same day must not overlap or touch (send 08:00–18:00 as one window, not 08:00–12:00 and 12:00–18:00), so a booked slot must fit inside a single window.
- An instant booking starts 30 minutes after it is made, which is the time for the maid to arrive. That slot must fall within the maid's availability, and the booking can be cancelled until the maid arrives.
- A recurring booking needs an end date and can span at most 90 days; the customer books again for a longer period. Occurrences already in the past are skipped.
- Skills and services are the same thing: a maid's skills are the services she offers (cleaning, cooking, dishwashing). Search takes one `services` parameter that covers both service type and skills.
- Price is `pricePerHour × duration`.
- Each occurrence is paid separately. Card numbers and UPI ids are validated but not stored.
- A visit can be paid before or after it starts.
- The payment gateway is mocked and always succeeds.
- Cancelling a paid occurrence refunds the full amount. Cancelling an unpaid occurrence refunds nothing. An occurrence that has already started cannot be cancelled. Cancelling a series cancels only its upcoming occurrences.
- The refund is taken before the occurrence is marked cancelled. If the gateway declines the refund, nothing changes and the cancellation can be retried.
- Rating is optional at onboarding, because there is no review flow. A maid without a rating is unrated: she is returned with `"rating": null` (not 0) and is not hidden by the `minRating` filter, so new maids can still be discovered and booked.
- `customerId` is taken as given, because there are no customer accounts or authentication.
- The service runs as a single instance.

## With more time

- **Multiple instances:** replace the in-JVM locks with database guarantees (an exclusion or unique constraint on maid and slot, or optimistic locking).
- **Payment idempotency:** accept an idempotency key so a retried request never charges twice.
- **Shorter lock hold:** don't hold the maid lock during the gateway call. Mark the occurrence as payment-in-progress instead, and refund if it was cancelled in the meantime.
- **Transactions and refund failures:** cancelling writes twice, first the refund and then the booking, with no transaction around them. With a database, wrap them in one transaction, record the refund as pending and send it to the gateway in the background (outbox), so a failure can never leave money refunded for a visit that is still active.
- **Ratings from reviews:** let customers rate completed visits and compute the rating as a weighted (Bayesian) average, so a single 5★ review does not outrank a long track record.
- **Richer policies:** a time-based cancellation policy (for example, partial refund within 24 hours) and a pricing strategy for surge or discounts.
- **Arrival time:** estimate the instant-booking lead time from the maid's distance to the customer instead of a fixed 30 minutes.
- **Timezones:** store a timezone per locality.
- **Search at scale:** move locality, service and price filters into database queries and keep only availability as an in-memory check.
- **API:** read endpoints for bookings and payments.
