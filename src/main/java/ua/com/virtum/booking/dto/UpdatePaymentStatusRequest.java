package ua.com.virtum.booking.dto;

import jakarta.validation.constraints.NotNull;
import ua.com.virtum.booking.entity.PaymentStatus;

public record UpdatePaymentStatusRequest(@NotNull PaymentStatus paymentStatus) {}
