package ua.com.virtum.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.virtum.booking.entity.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStartsAtLessThanAndEndsAtGreaterThan(LocalDateTime end, LocalDateTime start);
    List<Booking> findByStartsAtBetweenOrderByStartsAtAsc(LocalDateTime from, LocalDateTime to);
}
