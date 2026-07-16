package ua.com.virtum.booking.dto;

import ua.com.virtum.booking.entity.BookingStatus;
import ua.com.virtum.booking.entity.PaymentMethod;
import ua.com.virtum.booking.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminBookingResponse(
        Long id,
        String serviceSlug,
        String serviceTitle,
        Integer durationMinutes,
        Integer helmetsCount,
        BigDecimal price,
        String currency,
        String customerName,
        String customerPhone,
        String customerEmail,
        String customerComment,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        BookingStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        PaymentProofResponse paymentProof,
        LocalDateTime createdAt
) {}
