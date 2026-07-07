package ua.com.virtum.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityBlockResponse(
        Long id,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String reason,
        LocalDateTime createdAt
) {}
