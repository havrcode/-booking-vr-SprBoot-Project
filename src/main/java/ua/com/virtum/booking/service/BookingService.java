package ua.com.virtum.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.virtum.booking.dto.BookingResponse;
import ua.com.virtum.booking.dto.CreateBookingRequest;
import ua.com.virtum.booking.entity.Booking;
import ua.com.virtum.booking.entity.VrService;
import ua.com.virtum.booking.exception.ConflictException;
import ua.com.virtum.booking.exception.NotFoundException;
import ua.com.virtum.booking.repository.BookingRepository;
import ua.com.virtum.booking.repository.VrServiceRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final VrServiceRepository vrServiceRepository;

    public BookingService(BookingRepository bookingRepository, VrServiceRepository vrServiceRepository) {
        this.bookingRepository = bookingRepository;
        this.vrServiceRepository = vrServiceRepository;
    }

    public List<VrService> listServices() {
        return vrServiceRepository.findByActiveTrueOrderByTitleAsc();
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        VrService service = vrServiceRepository.findBySlugAndActiveTrue(request.serviceSlug())
                .orElseThrow(() -> new NotFoundException("Service not found: " + request.serviceSlug()));

        LocalDateTime startsAt = request.startsAt();
        LocalDateTime endsAt = startsAt.plusMinutes(service.getDurationMinutes());

        if (!bookingRepository.findByStartsAtLessThanAndEndsAtGreaterThan(endsAt, startsAt).isEmpty()) {
            throw new ConflictException("This time slot is already booked. Please choose another one.");
        }

        Booking booking = new Booking();
        booking.setService(service);
        booking.setCustomerName(request.customerName());
        booking.setCustomerPhone(request.customerPhone());
        booking.setCustomerEmail(request.customerEmail());
        booking.setStartsAt(startsAt);
        booking.setEndsAt(endsAt);

        return toResponse(bookingRepository.save(booking));
    }

    public List<BookingResponse> listDay(LocalDate day) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        return bookingRepository.findByStartsAtBetweenOrderByStartsAtAsc(from, to).stream()
                .map(this::toResponse)
                .toList();
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getService().getSlug(),
                b.getService().getTitle(),
                b.getService().getDurationMinutes(),
                b.getService().getPrice(),
                b.getCustomerName(),
                b.getCustomerPhone(),
                b.getCustomerEmail(),
                b.getStartsAt(),
                b.getEndsAt()
        );
    }
}
