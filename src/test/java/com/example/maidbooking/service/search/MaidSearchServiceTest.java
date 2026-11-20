package com.example.maidbooking.service.search;

import com.example.maidbooking.domain.AvailabilityWindow;
import com.example.maidbooking.domain.Booking;
import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.domain.Occurrence;
import com.example.maidbooking.domain.TimeSlot;
import com.example.maidbooking.dto.request.MaidSearchRequest;
import com.example.maidbooking.dto.response.MaidResponse;
import com.example.maidbooking.enums.BookingType;
import com.example.maidbooking.enums.Gender;
import com.example.maidbooking.enums.ServiceType;
import com.example.maidbooking.repository.inmemory.InMemoryBookingRepository;
import com.example.maidbooking.repository.inmemory.InMemoryMaidRepository;
import com.example.maidbooking.service.AvailabilityChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MaidSearchServiceTest {

    private static final LocalDateTime TUESDAY_9AM = LocalDateTime.of(2026, 12, 1, 9, 0);

    private final InMemoryMaidRepository maidRepository = new InMemoryMaidRepository();
    private final InMemoryBookingRepository bookingRepository = new InMemoryBookingRepository();
    private MaidSearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new MaidSearchService(maidRepository, List.of(
                new LocalityFilter(),
                new ServiceFilter(),
                new RatingFilter(),
                new PriceFilter(),
                new GenderFilter(),
                new AvailabilityFilter(new AvailabilityChecker(bookingRepository))));

        maidRepository.save(maid("sunita", Gender.FEMALE, "Koramangala",
                Set.of(ServiceType.CLEANING, ServiceType.COOKING), 300, 4.8, DayOfWeek.TUESDAY));
        maidRepository.save(maid("ravi", Gender.MALE, "Koramangala",
                Set.of(ServiceType.CLEANING), 200, 4.0, DayOfWeek.TUESDAY));
        maidRepository.save(maid("meena", Gender.FEMALE, "Indiranagar",
                Set.of(ServiceType.DISHWASHING), 150, 3.5, DayOfWeek.WEDNESDAY));
    }

    @Test
    void returnsAllMaidsWhenNoFiltersGiven() {
        assertEquals(Set.of("sunita", "ravi", "meena"), idsOf(searchService.search(new MaidSearchRequest())));
    }

    @Test
    void filtersByLocalityIgnoringCase() {
        MaidSearchRequest request = new MaidSearchRequest();
        request.setLocality("koramangala");

        assertEquals(Set.of("sunita", "ravi"), idsOf(searchService.search(request)));
    }

    @Test
    void blankLocalityIsIgnored() {
        MaidSearchRequest request = new MaidSearchRequest();
        request.setLocality(" ");

        assertEquals(Set.of("sunita", "ravi", "meena"), idsOf(searchService.search(request)));
    }

    @Test
    void combinesServiceRatingPriceAndGenderFilters() {
        MaidSearchRequest request = new MaidSearchRequest();
        request.setServices(Set.of(ServiceType.CLEANING));
        request.setMinRating(4.5);
        request.setMaxPrice(BigDecimal.valueOf(300));
        request.setGender(Gender.FEMALE);

        assertEquals(Set.of("sunita"), idsOf(searchService.search(request)));
    }

    @Test
    void unratedMaidIsNotHiddenByMinRating() {
        maidRepository.save(maid("asha", Gender.FEMALE, "Koramangala",
                Set.of(ServiceType.CLEANING), 250, null, DayOfWeek.TUESDAY));
        MaidSearchRequest request = new MaidSearchRequest();
        request.setMinRating(4.5);

        assertEquals(Set.of("sunita", "asha"), idsOf(searchService.search(request)));
    }

    @Test
    void serviceFilterRequiresAllRequestedServices() {
        MaidSearchRequest request = new MaidSearchRequest();
        request.setServices(Set.of(ServiceType.CLEANING, ServiceType.COOKING));

        assertEquals(Set.of("sunita"), idsOf(searchService.search(request)));
    }

    @Test
    void excludesMaidsNotWorkingOrAlreadyBookedAtRequestedTime() {
        bookingRepository.save(new Booking("b1", "c1", "ravi", ServiceType.CLEANING, BookingType.SCHEDULED,
                List.of(new Occurrence("o1", TimeSlot.of(TUESDAY_9AM, Duration.ofHours(2)),
                        BigDecimal.valueOf(400)))));

        assertEquals(Set.of("sunita"), idsOf(searchService.search(requestAt(TUESDAY_9AM.plusHours(1), 60))));
    }

    @Test
    void cancelledBookingMakesMaidDiscoverableAgain() {
        Booking booking = new Booking("b1", "c1", "ravi", ServiceType.CLEANING, BookingType.SCHEDULED,
                List.of(new Occurrence("o1", TimeSlot.of(TUESDAY_9AM, Duration.ofHours(2)),
                        BigDecimal.valueOf(400))));
        bookingRepository.save(booking);
        booking.getOccurrences().get(0).cancel(TUESDAY_9AM.minusDays(1));

        assertEquals(Set.of("sunita", "ravi"), idsOf(searchService.search(requestAt(TUESDAY_9AM, 60))));
    }

    @Test
    void startTimeWithoutDurationIsRejected() {
        MaidSearchRequest request = new MaidSearchRequest();
        request.setStartTime(TUESDAY_9AM);

        assertThrows(IllegalArgumentException.class, () -> searchService.search(request));
    }

    @Test
    void startTimeWithoutDurationIsRejectedEvenWhenAnotherFilterExcludesEveryMaid() {
        MaidSearchRequest request = new MaidSearchRequest();
        request.setLocality("Nowhere");
        request.setStartTime(TUESDAY_9AM);

        assertThrows(IllegalArgumentException.class, () -> searchService.search(request));
    }

    private static MaidSearchRequest requestAt(LocalDateTime start, int minutes) {
        MaidSearchRequest request = new MaidSearchRequest();
        request.setStartTime(start);
        request.setDurationMinutes(minutes);
        return request;
    }

    private static Maid maid(String id, Gender gender, String locality, Set<ServiceType> services,
                             int price, Double rating, DayOfWeek workingDay) {
        return new Maid(id, id, gender, locality, services, BigDecimal.valueOf(price), rating,
                List.of(new AvailabilityWindow(workingDay, LocalTime.of(8, 0), LocalTime.of(18, 0))));
    }

    private static Set<String> idsOf(List<MaidResponse> maids) {
        return Set.copyOf(maids.stream().map(MaidResponse::getId).toList());
    }
}
