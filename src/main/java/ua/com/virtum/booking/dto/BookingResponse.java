package ua.com.virtum.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        String serviceSlug,
        String serviceTitle,
        Integer durationMinutes,
        BigDecimal price,
        String customerName,
        String customerPhone,
        String customerEmail,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
