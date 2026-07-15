package ua.com.virtum.booking.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ua.com.virtum.booking.dto.BookingSettingsResponse;
import ua.com.virtum.booking.dto.BookingSlotResponse;
import ua.com.virtum.booking.dto.BookingResponse;
import ua.com.virtum.booking.dto.CreateBookingRequest;
import ua.com.virtum.booking.entity.VrService;
import ua.com.virtum.booking.service.BookingService;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/services")
    public List<VrService> services() {
        return bookingService.listServices();
    }

    @GetMapping("/booking-settings")
    public BookingSettingsResponse bookingSettings() {
        return bookingService.bookingSettings();
    }

    @PostMapping("/bookings")
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    @PostMapping(path = "/bookings/{id}/payment-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookingResponse uploadPaymentProof(
            @PathVariable Long id,
            @RequestParam String token,
            @RequestParam("file") MultipartFile file
    ) {
        return bookingService.uploadPaymentProof(id, token, file);
    }

    @GetMapping("/bookings")
    public List<BookingSlotResponse> dayBookings(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.listDay(date);
    }
}
