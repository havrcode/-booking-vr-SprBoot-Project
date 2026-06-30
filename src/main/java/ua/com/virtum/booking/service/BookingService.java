package ua.com.virtum.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.dto.AdminVrServiceResponse;
import ua.com.virtum.booking.dto.BookingResponse;
import ua.com.virtum.booking.dto.CreateBookingRequest;
import ua.com.virtum.booking.dto.SaveVrServiceRequest;
import ua.com.virtum.booking.entity.Booking;
import ua.com.virtum.booking.entity.BookingStatus;
import ua.com.virtum.booking.entity.VrService;
import ua.com.virtum.booking.exception.BadRequestException;
import ua.com.virtum.booking.exception.ConflictException;
import ua.com.virtum.booking.exception.NotFoundException;
import ua.com.virtum.booking.notification.BookingNotificationService;
import ua.com.virtum.booking.repository.BookingRepository;
import ua.com.virtum.booking.repository.VrServiceRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final VrServiceRepository vrServiceRepository;
    private final BookingNotificationService notificationService;

    public BookingService(
            BookingRepository bookingRepository,
            VrServiceRepository vrServiceRepository,
            BookingNotificationService notificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.vrServiceRepository = vrServiceRepository;
        this.notificationService = notificationService;
    }

    public List<VrService> listServices() {
        return vrServiceRepository.findByActiveTrueOrderByTitleAsc();
    }

    @Transactional(readOnly = true)
    public List<AdminVrServiceResponse> listAdminServices() {
        return vrServiceRepository.findAllByOrderByActiveDescTitleAsc().stream()
                .map(this::toAdminServiceResponse)
                .toList();
    }

    @Transactional
    public AdminVrServiceResponse createService(SaveVrServiceRequest request) {
        String slug = normalizeSlug(request.slug());

        if (vrServiceRepository.existsBySlug(slug)) {
            throw new ConflictException("Service slug already exists: " + slug);
        }

        VrService service = new VrService();
        applyServiceRequest(service, request, slug);
        return toAdminServiceResponse(vrServiceRepository.save(service));
    }

    @Transactional
    public AdminVrServiceResponse updateService(Long id, SaveVrServiceRequest request) {
        VrService service = vrServiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service not found: " + id));
        String slug = normalizeSlug(request.slug());

        if (vrServiceRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("Service slug already exists: " + slug);
        }

        applyServiceRequest(service, request, slug);
        return toAdminServiceResponse(service);
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request) {
        VrService service = vrServiceRepository.findBySlugAndActiveTrue(request.serviceSlug())
                .orElseThrow(() -> new NotFoundException("Service not found: " + request.serviceSlug()));

        LocalDateTime startsAt = request.startsAt();
        LocalDateTime endsAt = startsAt.plusMinutes(service.getDurationMinutes());

        // Only confirmed bookings block the calendar. Cancelled bookings keep history,
        // but their time slot can be booked again.
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

        Booking savedBooking = bookingRepository.save(booking);
        notificationService.bookingCreated(toAdminResponse(savedBooking));
        return toResponse(savedBooking);
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

        // The API accepts inclusive LocalDate filters. Repository queries use an exclusive
        // upper LocalDateTime boundary, so "to=2026-05-20" includes that whole day.
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
        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(status);
        AdminBookingResponse response = toAdminResponse(booking);

        if (previousStatus != status) {
            notificationService.bookingStatusChanged(response, previousStatus);
        }

        return response;
    }

    private void applyServiceRequest(VrService service, SaveVrServiceRequest request, String slug) {
        service.setSlug(slug);
        service.setTitle(request.title().trim());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        // Inactive services stay available for old bookings but disappear from the public site.
        service.setActive(request.active());
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    private AdminVrServiceResponse toAdminServiceResponse(VrService service) {
        return new AdminVrServiceResponse(
                service.getId(),
                service.getSlug(),
                service.getTitle(),
                service.getDurationMinutes(),
                service.getPrice(),
                service.isActive()
        );
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
