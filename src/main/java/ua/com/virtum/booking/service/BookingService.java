package ua.com.virtum.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ua.com.virtum.booking.config.BookingProperties;
import ua.com.virtum.booking.config.PaymentProperties;
import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.dto.AdminVrServiceResponse;
import ua.com.virtum.booking.dto.AvailabilityBlockResponse;
import ua.com.virtum.booking.dto.BookingResponse;
import ua.com.virtum.booking.dto.BookingSettingsResponse;
import ua.com.virtum.booking.dto.BookingSlotResponse;
import ua.com.virtum.booking.dto.CreateBookingRequest;
import ua.com.virtum.booking.dto.PaymentProofResponse;
import ua.com.virtum.booking.dto.PaymentSettingsResponse;
import ua.com.virtum.booking.dto.SaveAvailabilityBlockRequest;
import ua.com.virtum.booking.dto.SaveVrServiceRequest;
import ua.com.virtum.booking.entity.AvailabilityBlock;
import ua.com.virtum.booking.entity.Booking;
import ua.com.virtum.booking.entity.BookingStatus;
import ua.com.virtum.booking.entity.PaymentMethod;
import ua.com.virtum.booking.entity.PaymentStatus;
import ua.com.virtum.booking.entity.VrService;
import ua.com.virtum.booking.exception.BadRequestException;
import ua.com.virtum.booking.exception.ConflictException;
import ua.com.virtum.booking.exception.NotFoundException;
import ua.com.virtum.booking.notification.BookingNotificationService;
import ua.com.virtum.booking.repository.AvailabilityBlockRepository;
import ua.com.virtum.booking.repository.BookingRepository;
import ua.com.virtum.booking.repository.VrServiceRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BookingService {
    private final BookingRepository bookingRepository;
    private final VrServiceRepository vrServiceRepository;
    private final AvailabilityBlockRepository availabilityBlockRepository;
    private final BookingNotificationService notificationService;
    private final BookingProperties bookingProperties;
    private final PaymentProperties paymentProperties;
    private final PaymentProofStorage paymentProofStorage;

    public BookingService(
            BookingRepository bookingRepository,
            VrServiceRepository vrServiceRepository,
            AvailabilityBlockRepository availabilityBlockRepository,
            BookingNotificationService notificationService,
            BookingProperties bookingProperties,
            PaymentProperties paymentProperties,
            PaymentProofStorage paymentProofStorage
    ) {
        this.bookingRepository = bookingRepository;
        this.vrServiceRepository = vrServiceRepository;
        this.availabilityBlockRepository = availabilityBlockRepository;
        this.notificationService = notificationService;
        this.bookingProperties = bookingProperties;
        this.paymentProperties = paymentProperties;
        this.paymentProofStorage = paymentProofStorage;
    }

    public List<VrService> listServices() {
        return vrServiceRepository.findByActiveTrueOrderByTitleAsc();
    }

    public BookingSettingsResponse bookingSettings() {
        return new BookingSettingsResponse(
                bookingProperties.getMaxConcurrentBookings(),
                paymentSettings()
        );
    }

    public PaymentSettingsResponse paymentSettings() {
        return new PaymentSettingsResponse(
                paymentProperties.isPayAtClubEnabled(),
                paymentProperties.isCardTransferEnabled(),
                blankToNull(paymentProperties.getCardHolder()),
                blankToNull(paymentProperties.getCardNumber()),
                blankToNull(paymentProperties.getCardBank()),
                blankToNull(paymentProperties.getCardTransferNote()),
                paymentProperties.getMaxProofSizeBytes()
        );
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

        // Only confirmed bookings block capacity. Cancelled bookings keep history,
        // but their time slot can be booked again.
        long overlappingBookings = bookingRepository.countByStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                BookingStatus.CONFIRMED,
                endsAt,
                startsAt
        );

        if (overlappingBookings >= bookingProperties.getMaxConcurrentBookings()) {
            throw new ConflictException("This time slot is fully booked. Please choose another one.");
        }

        if (!availabilityBlockRepository.findByStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
                endsAt,
                startsAt
        ).isEmpty()) {
            throw new ConflictException("This time slot is closed by administrator. Please choose another one.");
        }

        Booking booking = new Booking();
        booking.setService(service);
        booking.setCustomerName(request.customerName());
        booking.setCustomerPhone(request.customerPhone());
        booking.setCustomerEmail(request.customerEmail());
        booking.setStartsAt(startsAt);
        booking.setEndsAt(endsAt);
        booking.setPaymentMethod(resolvePaymentMethod(request.paymentMethod()));
        booking.setPaymentStatus(PaymentStatus.UNPAID);
        booking.setPaymentUploadToken(generatePaymentUploadToken());

        Booking savedBooking = bookingRepository.save(booking);
        notificationService.bookingCreated(toAdminResponse(savedBooking));
        return toResponse(savedBooking);
    }

    @Transactional
    public BookingResponse uploadPaymentProof(Long id, String token, MultipartFile file) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));

        if (token == null || token.isBlank() || !token.equals(booking.getPaymentUploadToken())) {
            throw new BadRequestException("Payment proof upload token is invalid.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot upload payment proof for cancelled booking.");
        }

        PaymentProofStorage.StoredPaymentProof storedProof = paymentProofStorage.store(booking, file);
        booking.setPaymentProofPath(storedProof.filename());
        booking.setPaymentProofOriginalFilename(storedProof.originalFilename());
        booking.setPaymentProofContentType(storedProof.contentType());
        booking.setPaymentProofUploadedAt(LocalDateTime.now());
        booking.setPaymentStatus(PaymentStatus.PENDING_REVIEW);

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingSlotResponse> listDay(LocalDate day) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        return bookingRepository.findByStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        BookingStatus.CONFIRMED,
                        from,
                        to
                ).stream()
                .map(this::toSlotResponse)
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

    @Transactional(readOnly = true)
    public List<AvailabilityBlockResponse> listDayAvailabilityBlocks(LocalDate day) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        return availabilityBlockRepository.findByStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(to, from).stream()
                .map(this::toPublicAvailabilityBlockResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailabilityBlockResponse> listAdminAvailabilityBlocks(LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from == null ? LocalDate.now() : from;
        LocalDate effectiveTo = to == null ? effectiveFrom.plusDays(30) : to;

        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new BadRequestException("Parameter 'to' must be on or after 'from'.");
        }

        LocalDateTime startsFrom = effectiveFrom.atStartOfDay();
        LocalDateTime startsBefore = effectiveTo.plusDays(1).atStartOfDay();

        return availabilityBlockRepository
                .findByStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(startsBefore, startsFrom)
                .stream()
                .map(this::toAvailabilityBlockResponse)
                .toList();
    }

    @Transactional
    public AvailabilityBlockResponse createAvailabilityBlock(SaveAvailabilityBlockRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("Field 'endsAt' must be after 'startsAt'.");
        }

        AvailabilityBlock block = new AvailabilityBlock();
        block.setStartsAt(request.startsAt());
        block.setEndsAt(request.endsAt());
        block.setReason(normalizeReason(request.reason()));
        return toAvailabilityBlockResponse(availabilityBlockRepository.save(block));
    }

    @Transactional
    public void deleteAvailabilityBlock(Long id) {
        if (!availabilityBlockRepository.existsById(id)) {
            throw new NotFoundException("Availability block not found: " + id);
        }

        availabilityBlockRepository.deleteById(id);
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

    @Transactional
    public AdminBookingResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
        booking.setPaymentStatus(paymentStatus);
        return toAdminResponse(booking);
    }

    @Transactional(readOnly = true)
    public Booking paymentProofBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));

        if (booking.getPaymentProofPath() == null || booking.getPaymentProofPath().isBlank()) {
            throw new NotFoundException("Payment proof not found for booking: " + id);
        }

        return booking;
    }

    private PaymentMethod resolvePaymentMethod(PaymentMethod paymentMethod) {
        PaymentMethod effectivePaymentMethod = paymentMethod == null ? PaymentMethod.PAY_AT_CLUB : paymentMethod;

        if (effectivePaymentMethod == PaymentMethod.PAY_AT_CLUB && !paymentProperties.isPayAtClubEnabled()) {
            throw new BadRequestException("Pay at club is not available.");
        }

        if (effectivePaymentMethod == PaymentMethod.CARD_TRANSFER && !paymentProperties.isCardTransferEnabled()) {
            throw new BadRequestException("Card transfer is not available.");
        }

        return effectivePaymentMethod;
    }

    private String generatePaymentUploadToken() {
        return UUID.randomUUID().toString().replace("-", "");
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

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Закрито адміністратором";
        }

        return reason.trim();
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
                b.getStatus(),
                b.getPaymentMethod(),
                b.getPaymentStatus(),
                b.getPaymentUploadToken()
        );
    }

    private BookingSlotResponse toSlotResponse(Booking b) {
        return new BookingSlotResponse(
                b.getStartsAt(),
                b.getEndsAt()
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
                b.getPaymentMethod(),
                b.getPaymentStatus(),
                toPaymentProofResponse(b),
                b.getCreatedAt()
        );
    }

    private PaymentProofResponse toPaymentProofResponse(Booking b) {
        boolean uploaded = b.getPaymentProofPath() != null && !b.getPaymentProofPath().isBlank();
        return new PaymentProofResponse(
                uploaded,
                b.getPaymentProofOriginalFilename(),
                b.getPaymentProofContentType(),
                b.getPaymentProofUploadedAt(),
                uploaded ? "/api/v1/admin/bookings/%d/payment-proof".formatted(b.getId()) : null
        );
    }

    private AvailabilityBlockResponse toAvailabilityBlockResponse(AvailabilityBlock block) {
        return new AvailabilityBlockResponse(
                block.getId(),
                block.getStartsAt(),
                block.getEndsAt(),
                block.getReason(),
                block.getCreatedAt()
        );
    }

    private AvailabilityBlockResponse toPublicAvailabilityBlockResponse(AvailabilityBlock block) {
        return new AvailabilityBlockResponse(
                block.getId(),
                block.getStartsAt(),
                block.getEndsAt(),
                null,
                null
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
