package com.example.maidbooking.repository;

import com.example.maidbooking.domain.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String id);

    List<Booking> findByMaidId(String maidId);
}
