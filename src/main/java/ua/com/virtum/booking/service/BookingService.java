package ua.com.virtum.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.dto.BookingResponse;
import ua.com.virtum.booking.dto.CreateBookingRequest;
import ua.com.virtum.booking.entity.Booking;
import ua.com.virtum.booking.entity.BookingStatus;
import ua.com.virtum.booking.entity.VrService;
import ua.com.virtum.booking.exception.BadRequestException;
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

        if (!bookingRepository.findByStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                BookingStatus.CONFIRMED,
                endsAt,
                startsAt
        ).isEmpty()) {
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

    @Transactional(readOnly = true)
    public List<BookingResponse> listDay(LocalDate day) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        return bookingRepository.findByStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        BookingStatus.CONFIRMED,
                        from,
                        to
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> listAdmin(LocalDate from, LocalDate to, BookingStatus status) {
        LocalDate effectiveFrom = from == null ? LocalDate.now() : from;
        LocalDate effectiveTo = to == null ? effectiveFrom.plusDays(30) : to;

        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new BadRequestException("Parameter 'to' must be on or after 'from'.");
        }

        LocalDateTime startsFrom = effectiveFrom.atStartOfDay();
        LocalDateTime startsBefore = effectiveTo.plusDays(1).atStartOfDay();

        List<Booking> bookings = status == null
                ? bookingRepository.findByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        startsFrom,
                        startsBefore
                )
                : bookingRepository.findByStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        status,
                        startsFrom,
                        startsBefore
                );

        return bookings.stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public AdminBookingResponse updateStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
        booking.setStatus(status);
        return toAdminResponse(booking);
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
                b.getEndsAt(),
                b.getStatus()
        );
    }

    private AdminBookingResponse toAdminResponse(Booking b) {
        return new AdminBookingResponse(
                b.getId(),
                b.getService().getSlug(),
                b.getService().getTitle(),
                b.getService().getDurationMinutes(),
                b.getService().getPrice(),
                b.getCustomerName(),
                b.getCustomerPhone(),
                b.getCustomerEmail(),
                b.getStartsAt(),
                b.getEndsAt(),
                b.getStatus(),
                b.getCreatedAt()
        );
    }
}
