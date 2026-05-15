package ua.com.virtum.booking.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CreateBookingRequest(
        @NotBlank String serviceSlug,
        @NotBlank @Size(min = 2, max = 120) String customerName,
        @NotBlank @Pattern(regexp = "^[+0-9 ()-]{7,20}$") String customerPhone,
        @NotBlank @Email String customerEmail,
        @NotNull @Future LocalDateTime startsAt
) {}
