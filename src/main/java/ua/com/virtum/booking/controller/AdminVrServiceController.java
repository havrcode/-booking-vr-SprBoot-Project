package ua.com.virtum.booking.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ua.com.virtum.booking.dto.AdminVrServiceResponse;
import ua.com.virtum.booking.dto.SaveVrServiceRequest;
import ua.com.virtum.booking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/services")
public class AdminVrServiceController {
    private final BookingService bookingService;

    public AdminVrServiceController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<AdminVrServiceResponse> list() {
        return bookingService.listAdminServices();
    }

    @PostMapping
    public AdminVrServiceResponse create(@Valid @RequestBody SaveVrServiceRequest request) {
        return bookingService.createService(request);
    }

    @PutMapping("/{id}")
    public AdminVrServiceResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SaveVrServiceRequest request
    ) {
        return bookingService.updateService(id, request);
    }
}
