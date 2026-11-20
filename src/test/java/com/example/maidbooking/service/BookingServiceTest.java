package com.example.maidbooking.service;

import com.example.maidbooking.domain.AvailabilityWindow;
import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.dto.request.CreateBookingRequest;
import com.example.maidbooking.dto.response.BookingResponse;
import com.example.maidbooking.enums.BookingType;
import com.example.maidbooking.enums.Gender;
import com.example.maidbooking.enums.ServiceType;
import com.example.maidbooking.exception.SlotUnavailableException;
import com.example.maidbooking.repository.inmemory.InMemoryBookingRepository;
import com.example.maidbooking.repository.inmemory.InMemoryMaidRepository;
import com.example.maidbooking.service.slot.InstantSlotGenerator;
import com.example.maidbooking.service.slot.RecurringSlotGenerator;
import com.example.maidbooking.service.slot.ScheduledSlotGenerator;
import com.example.maidbooking.service.slot.SlotGeneratorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 30, 10, 0);
    private static final LocalDateTime TOMORROW_9AM = LocalDateTime.of(2026, 12, 1, 9, 0);

    private final Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
    private final InMemoryMaidRepository maidRepository = new InMemoryMaidRepository();
    private final InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        SlotGeneratorRegistry registry = new SlotGeneratorRegistry(List.of(
                new InstantSlotGenerator(clock),
                new ScheduledSlotGenerator(clock),
                new RecurringSlotGenerator(clock)));
        bookingService = new BookingService(new MaidService(maidRepository), bookingRepository,
                registry, new AvailabilityChecker(bookingRepository), new MaidLocks());

        List<AvailabilityWindow> everyDay = Arrays.stream(DayOfWeek.values())
                .map(day -> new AvailabilityWindow(day, LocalTime.of(8, 0), LocalTime.of(20, 0)))
                .toList();
        maidRepository.save(new Maid("m1", "Sunita", Gender.FEMALE, "Koramangala",
                Set.of(ServiceType.CLEANING), BigDecimal.valueOf(250), 4.5, everyDay));
        maidRepository.save(new Maid("m2", "Ravi", Gender.MALE, "Koramangala",
                Set.of(ServiceType.CLEANING), BigDecimal.valueOf(200), 4.0, everyDay));
    }

    @Test
    void scheduledBookingIsPricedByDuration() {
        BookingResponse booking = bookingService.book(scheduled(TOMORROW_9AM, 90));

        assertEquals(1, booking.getOccurrences().size());
        assertEquals(new BigDecimal("375.00"), booking.getOccurrences().get(0).getAmount());
    }

    @Test
    void instantBookingStartsAfterArrivalTime() {
        CreateBookingRequest request = baseRequest(BookingType.INSTANT, 60);

        BookingResponse booking = bookingService.book(request);

        assertEquals(NOW.plusMinutes(30), booking.getOccurrences().get(0).getStart());
    }

    @Test
    void rejectsSlotOutsideMaidAvailability() {
        assertThrows(SlotUnavailableException.class,
                () -> bookingService.book(scheduled(TOMORROW_9AM.withHour(19), 120)));
    }

    @Test
    void rejectsServiceMaidDoesNotOffer() {
        CreateBookingRequest request = scheduled(TOMORROW_9AM, 60);
        request.setServiceType(ServiceType.COOKING);

        assertThrows(IllegalArgumentException.class, () -> bookingService.book(request));
    }

    @Test
    void rejectsOverlappingBookingForSameMaid() {
        bookingService.book(scheduled(TOMORROW_9AM, 120));

        assertThrows(SlotUnavailableException.class,
                () -> bookingService.book(scheduled(TOMORROW_9AM.plusHours(1), 60)));
    }

    @Test
    void allowsBackToBackBookings() {
        bookingService.book(scheduled(TOMORROW_9AM, 60));

        BookingResponse next = bookingService.book(scheduled(TOMORROW_9AM.plusHours(1), 60));

        assertEquals(TOMORROW_9AM.plusHours(1), next.getOccurrences().get(0).getStart());
    }

    @Test
    void differentMaidsCanBeBookedForTheSameSlot() {
        bookingService.book(scheduled(TOMORROW_9AM, 60));

        CreateBookingRequest otherMaid = scheduled(TOMORROW_9AM, 60);
        otherMaid.setMaidId("m2");

        assertEquals("m2", bookingService.book(otherMaid).getMaidId());
    }

    @Test
    void recurringBookingIsRejectedWhenAnyOccurrenceClashes() {
        bookingService.book(scheduled(LocalDateTime.of(2026, 12, 8, 9, 30), 60));

        CreateBookingRequest recurring = baseRequest(BookingType.RECURRING, 60);
        recurring.setDaysOfWeek(Set.of(DayOfWeek.TUESDAY));
        recurring.setTimeOfDay(LocalTime.of(9, 0));
        recurring.setStartDate(LocalDate.of(2026, 12, 1));
        recurring.setEndDate(LocalDate.of(2026, 12, 15));

        assertThrows(SlotUnavailableException.class, () -> bookingService.book(recurring));
        assertEquals(1, bookingRepository.findByMaidId("m1").size());
    }

    @Test
    void cancelledBookingFreesTheSlot() {
        BookingResponse first = bookingService.book(scheduled(TOMORROW_9AM, 60));
        bookingService.getBooking(first.getId()).getOccurrences().get(0).cancel(NOW);

        BookingResponse second = bookingService.book(scheduled(TOMORROW_9AM, 60));

        assertEquals(TOMORROW_9AM, second.getOccurrences().get(0).getStart());
    }

    @Test
    void onlyOneOfConcurrentBookingsForSameSlotSucceeds() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            Callable<Boolean> attempt = () -> {
                start.await();
                try {
                    bookingService.book(scheduled(TOMORROW_9AM, 60));
                    return true;
                } catch (SlotUnavailableException e) {
                    return false;
                }
            };
            results.add(executor.submit(attempt));
        }
        start.countDown();

        int successes = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                successes++;
            }
        }
        executor.shutdown();

        assertEquals(1, successes);
        assertEquals(1, bookingRepository.findByMaidId("m1").size());
    }

    private CreateBookingRequest scheduled(LocalDateTime startTime, int minutes) {
        CreateBookingRequest request = baseRequest(BookingType.SCHEDULED, minutes);
        request.setStartTime(startTime);
        return request;
    }

    private CreateBookingRequest baseRequest(BookingType type, int minutes) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setCustomerId("c1");
        request.setMaidId("m1");
        request.setServiceType(ServiceType.CLEANING);
        request.setBookingType(type);
        request.setDurationMinutes(minutes);
        return request;
    }
}
