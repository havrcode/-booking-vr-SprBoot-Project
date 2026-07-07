package ua.com.virtum.booking.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.com.virtum.booking.dto.AvailabilityBlockResponse;
import ua.com.virtum.booking.service.BookingService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/availability-blocks")
public class AvailabilityBlockController {
    private final BookingService bookingService;

    public AvailabilityBlockController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<AvailabilityBlockResponse> dayBlocks(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return bookingService.listDayAvailabilityBlocks(date);
    }
}
