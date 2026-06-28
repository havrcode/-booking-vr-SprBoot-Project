package ua.com.virtum.booking.dto;

import jakarta.validation.constraints.NotNull;
import ua.com.virtum.booking.entity.BookingStatus;

public record UpdateBookingStatusRequest(@NotNull BookingStatus status) {}

