package ua.com.virtum.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ua.com.virtum.booking.entity.BookingStatus;
import ua.com.virtum.booking.entity.PaymentMethod;
import ua.com.virtum.booking.entity.PaymentStatus;

public record BookingResponse(
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
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        BookingStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String paymentUploadToken
) {}
