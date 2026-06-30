package ua.com.virtum.booking.dto;

import java.math.BigDecimal;

public record AdminVrServiceResponse(
        Long id,
        String slug,
        String title,
        Integer durationMinutes,
        BigDecimal price,
        boolean active
) {}
