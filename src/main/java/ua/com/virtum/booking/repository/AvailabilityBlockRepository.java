package ua.com.virtum.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.virtum.booking.entity.AvailabilityBlock;

import java.time.LocalDateTime;
import java.util.List;

public interface AvailabilityBlockRepository extends JpaRepository<AvailabilityBlock, Long> {
    List<AvailabilityBlock> findByStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
            LocalDateTime end,
            LocalDateTime start
    );
}
