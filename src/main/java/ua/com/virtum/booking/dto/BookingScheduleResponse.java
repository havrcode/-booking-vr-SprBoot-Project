package ua.com.virtum.booking.dto;

public record BookingScheduleResponse(
        String openTime,
        String closeTime,
        String breakStart,
        String breakEnd,
        int slotStepMinutes
) {}
