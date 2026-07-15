package ua.com.virtum.booking.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.dto.UpdatePaymentStatusRequest;
import ua.com.virtum.booking.dto.UpdateBookingStatusRequest;
import ua.com.virtum.booking.entity.Booking;
import ua.com.virtum.booking.entity.BookingStatus;
import ua.com.virtum.booking.service.BookingService;
import ua.com.virtum.booking.service.PaymentProofStorage;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {
    private final BookingService bookingService;
    private final PaymentProofStorage paymentProofStorage;

    public AdminBookingController(BookingService bookingService, PaymentProofStorage paymentProofStorage) {
        this.bookingService = bookingService;
        this.paymentProofStorage = paymentProofStorage;
    }

    @GetMapping
    public List<AdminBookingResponse> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,
            @RequestParam(required = false)
            BookingStatus status
    ) {
        return bookingService.listAdmin(from, to, status);
    }

    @PatchMapping("/{id}/status")
    public AdminBookingResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request
    ) {
        return bookingService.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/payment-status")
    public AdminBookingResponse updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        return bookingService.updatePaymentStatus(id, request.paymentStatus());
    }

    @GetMapping("/{id}/payment-proof")
    public ResponseEntity<Resource> paymentProof(@PathVariable Long id) {
        Booking booking = bookingService.paymentProofBooking(id);
        Resource resource = paymentProofStorage.load(booking.getPaymentProofPath());
        MediaType contentType = booking.getPaymentProofContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(booking.getPaymentProofContentType());

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(booking.getPaymentProofOriginalFilename())
                                .build()
                                .toString()
                )
                .body(resource);
    }
}
