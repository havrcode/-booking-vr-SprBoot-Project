package ua.com.virtum.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.com.virtum.booking.entity.Booking;
import ua.com.virtum.booking.entity.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    long countByStatusAndStartsAtLessThanAndEndsAtGreaterThan(
            BookingStatus status,
            LocalDateTime end,
            LocalDateTime start
    );

    @Query("""
            select coalesce(sum(b.helmetsCount), 0)
            from Booking b
            where b.status = :status
                and b.startsAt < :end
                and b.endsAt > :start
            """)
    long sumHelmetsCountByStatusAndPeriod(
            @Param("status") BookingStatus status,
            @Param("end") LocalDateTime end,
            @Param("start") LocalDateTime start
    );

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
