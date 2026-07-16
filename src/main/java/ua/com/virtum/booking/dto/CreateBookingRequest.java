package ua.com.virtum.booking.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import ua.com.virtum.booking.entity.PaymentMethod;

public record CreateBookingRequest(
        @NotBlank(message = "Оберіть послугу.") String serviceSlug,
        @NotBlank(message = "Вкажіть ім'я.") @Size(min = 2, max = 120, message = "Ім'я має містити від 2 до 120 символів.") String customerName,
        @NotBlank(message = "Вкажіть телефон.") @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "Вкажіть коректний номер телефону.") String customerPhone,
        @Email(message = "Вкажіть коректний email або залиште поле порожнім.") @Size(max = 255, message = "Email занадто довгий.") String customerEmail,
        @Size(max = 500, message = "Коментар має бути до 500 символів.") String customerComment,
        @NotNull(message = "Оберіть час бронювання.") LocalDateTime startsAt,
        @Min(1) Integer helmetsCount,
        PaymentMethod paymentMethod
) {}
