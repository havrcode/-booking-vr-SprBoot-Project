package ua.com.virtum.booking.dto;

import java.time.LocalDateTime;

public record PaymentProofResponse(
        boolean uploaded,
        String originalFilename,
        String contentType,
        LocalDateTime uploadedAt,
        String url
) {}
