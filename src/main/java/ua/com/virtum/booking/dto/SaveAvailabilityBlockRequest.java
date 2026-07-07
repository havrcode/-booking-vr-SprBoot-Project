package ua.com.virtum.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SaveAvailabilityBlockRequest(
        @NotNull @Future LocalDateTime startsAt,
        @NotNull @Future LocalDateTime endsAt,
        @Size(max = 255) String reason
) {}
