package ua.com.virtum.booking.dto;

public record BookingSettingsResponse(
        int maxConcurrentBookings,
        PaymentSettingsResponse payment,
        BookingScheduleResponse schedule
) {}
