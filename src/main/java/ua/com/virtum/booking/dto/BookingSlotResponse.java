package ua.com.virtum.booking.dto;

import java.time.LocalDateTime;

public record BookingSlotResponse(
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer helmetsCount
) {}
