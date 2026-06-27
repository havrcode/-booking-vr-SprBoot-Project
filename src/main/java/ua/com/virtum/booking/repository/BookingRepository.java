package ua.com.virtum.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.virtum.booking.entity.Booking;
import ua.com.virtum.booking.entity.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatusAndStartsAtLessThanAndEndsAtGreaterThan(
            BookingStatus status,
            LocalDateTime end,
            LocalDateTime start
    );

    List<Booking> findByStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            BookingStatus status,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Booking> findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            LocalDateTime from,
            LocalDateTime to
    );
}
