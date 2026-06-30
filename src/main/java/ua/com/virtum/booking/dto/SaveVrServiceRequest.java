package ua.com.virtum.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaveVrServiceRequest(
        @NotBlank
        @Size(max = 120)
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "must contain only lowercase letters, numbers, and single hyphens"
        )
        String slug,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotNull
        @Min(15)
        @Max(480)
        Integer durationMinutes,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @NotNull
        Boolean active
) {}
