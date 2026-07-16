package ua.com.virtum.booking.dto;

import java.time.LocalDate;

public record BookingDayAvailabilityResponse(
        LocalDate date,
        boolean available,
        int availableSlots
) {}
