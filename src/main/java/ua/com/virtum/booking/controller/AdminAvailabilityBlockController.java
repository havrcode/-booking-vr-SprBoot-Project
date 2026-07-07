package ua.com.virtum.booking.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ua.com.virtum.booking.dto.AvailabilityBlockResponse;
import ua.com.virtum.booking.dto.SaveAvailabilityBlockRequest;
import ua.com.virtum.booking.service.BookingService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/availability-blocks")
public class AdminAvailabilityBlockController {
    private final BookingService bookingService;

    public AdminAvailabilityBlockController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<AvailabilityBlockResponse> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return bookingService.listAdminAvailabilityBlocks(from, to);
    }

    @PostMapping
    public AvailabilityBlockResponse create(@Valid @RequestBody SaveAvailabilityBlockRequest request) {
        return bookingService.createAvailabilityBlock(request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        bookingService.deleteAvailabilityBlock(id);
    }
}
