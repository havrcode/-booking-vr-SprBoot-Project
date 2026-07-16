package ua.com.virtum.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ua.com.virtum.booking.config.BookingProperties;
import ua.com.virtum.booking.config.PaymentProperties;
import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.dto.AdminVrServiceResponse;
import ua.com.virtum.booking.dto.AvailabilityBlockResponse;
import ua.com.virtum.booking.dto.BookingDayAvailabilityResponse;
import ua.com.virtum.booking.dto.BookingResponse;
import ua.com.virtum.booking.dto.BookingScheduleResponse;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BookingService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final BigDecimal WEEKDAY_HOURLY_PRICE = new BigDecimal("400.00");
    private static final BigDecimal WEEKEND_HOURLY_PRICE = new BigDecimal("500.00");

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
                paymentSettings(),
                bookingSchedule()
        );
    }

    public BookingScheduleResponse bookingSchedule() {
        return new BookingScheduleResponse(
                formatTime(bookingProperties.getOpenTime()),
                formatTime(bookingProperties.getCloseTime()),
                formatTime(bookingProperties.getBreakStart()),
                formatTime(bookingProperties.getBreakEnd()),
                bookingProperties.getSlotStepMinutes()
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
        int helmetsCount = resolveHelmetsCount(request.helmetsCount());

        validateWithinBookingSchedule(startsAt, endsAt);

        // Only confirmed bookings block capacity. Cancelled bookings keep history,
        // but their time slot can be booked again.
        long overlappingHelmets = bookingRepository.sumHelmetsCountByStatusAndPeriod(
                BookingStatus.CONFIRMED,
                endsAt,
                startsAt
        );

        if (overlappingHelmets + helmetsCount > bookingProperties.getMaxConcurrentBookings()) {
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
        booking.setHelmetsCount(helmetsCount);
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
    public List<BookingDayAvailabilityResponse> listDayAvailability(
            LocalDate from,
            LocalDate to,
            String serviceSlug,
            Integer helmetsCount
    ) {
        LocalDate effectiveFrom = from == null ? LocalDate.now() : from;
        LocalDate effectiveTo = to == null ? effectiveFrom.plusMonths(2) : to;

        if (serviceSlug == null || serviceSlug.isBlank()) {
            throw new BadRequestException("Parameter 'serviceSlug' is required.");
        }

        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new BadRequestException("Parameter 'to' must be on or after 'from'.");
        }

        if (effectiveTo.isAfter(effectiveFrom.plusDays(62))) {
            throw new BadRequestException("Date range cannot be longer than 62 days.");
        }

        VrService service = vrServiceRepository.findBySlugAndActiveTrue(serviceSlug)
                .orElseThrow(() -> new NotFoundException("Service not found: " + serviceSlug));
        int requestedHelmets = resolveHelmetsCount(helmetsCount);
        LocalDateTime startsFrom = effectiveFrom.atStartOfDay();
        LocalDateTime startsBefore = effectiveTo.plusDays(1).atStartOfDay();
        List<Booking> bookings = bookingRepository
                .findByStatusAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                        BookingStatus.CONFIRMED,
                        startsFrom,
                        startsBefore
                );
        List<AvailabilityBlock> availabilityBlocks = availabilityBlockRepository
                .findByStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(startsBefore, startsFrom);

        return effectiveFrom.datesUntil(effectiveTo.plusDays(1))
                .map(day -> toDayAvailability(day, service.getDurationMinutes(), requestedHelmets, bookings, availabilityBlocks))
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

    private int resolveHelmetsCount(Integer helmetsCount) {
        int effectiveHelmetsCount = helmetsCount == null ? 1 : helmetsCount;

        if (effectiveHelmetsCount < 1) {
            throw new BadRequestException("Field 'helmetsCount' must be at least 1.");
        }

        if (effectiveHelmetsCount > bookingProperties.getMaxConcurrentBookings()) {
            throw new BadRequestException("Cannot book more helmets than available.");
        }

        return effectiveHelmetsCount;
    }

    private void validateWithinBookingSchedule(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (!startsAt.toLocalDate().equals(endsAt.toLocalDate())) {
            throw new ConflictException("This time slot is outside working hours. Please choose another one.");
        }

        LocalTime startTime = startsAt.toLocalTime();
        LocalTime endTime = endsAt.toLocalTime();
        LocalTime openTime = bookingProperties.getOpenTime();
        LocalTime closeTime = bookingProperties.getCloseTime();

        if (startTime.isBefore(openTime) || endTime.isAfter(closeTime)) {
            throw new ConflictException("This time slot is outside working hours. Please choose another one.");
        }

        int minutesFromOpen = toMinutes(startTime) - toMinutes(openTime);
        if (minutesFromOpen % bookingProperties.getSlotStepMinutes() != 0) {
            throw new ConflictException("This time slot is not aligned with booking slot step. Please choose another one.");
        }

        if (overlapsBreak(startTime, endTime)) {
            throw new ConflictException("This time slot is closed for lunch break. Please choose another one.");
        }
    }

    private boolean overlapsBreak(LocalTime startTime, LocalTime endTime) {
        LocalTime breakStart = bookingProperties.getBreakStart();
        LocalTime breakEnd = bookingProperties.getBreakEnd();

        if (!breakEnd.isAfter(breakStart)) {
            return false;
        }

        return startTime.isBefore(breakEnd) && endTime.isAfter(breakStart);
    }

    private BookingDayAvailabilityResponse toDayAvailability(
            LocalDate day,
            int durationMinutes,
            int helmetsCount,
            List<Booking> bookings,
            List<AvailabilityBlock> availabilityBlocks
    ) {
        int availableSlots = countAvailableSlots(day, durationMinutes, helmetsCount, bookings, availabilityBlocks);
        return new BookingDayAvailabilityResponse(day, availableSlots > 0, availableSlots);
    }

    private int countAvailableSlots(
            LocalDate day,
            int durationMinutes,
            int helmetsCount,
            List<Booking> bookings,
            List<AvailabilityBlock> availabilityBlocks
    ) {
        int slots = 0;
        int opensAt = toMinutes(bookingProperties.getOpenTime());
        int closesAt = toMinutes(bookingProperties.getCloseTime());
        int stepMinutes = bookingProperties.getSlotStepMinutes();
        LocalDateTime now = LocalDateTime.now();

        for (int minute = opensAt; minute + durationMinutes <= closesAt; minute += stepMinutes) {
            LocalDateTime startsAt = day.atTime(minute / 60, minute % 60);
            LocalDateTime endsAt = startsAt.plusMinutes(durationMinutes);

            if (startsAt.isBefore(now) || startsAt.isEqual(now)) {
                continue;
            }

            if (overlapsBreak(startsAt.toLocalTime(), endsAt.toLocalTime())) {
                continue;
            }

            if (overlapsAvailabilityBlock(startsAt, endsAt, availabilityBlocks)) {
                continue;
            }

            if (usedHelmets(startsAt, endsAt, bookings) + helmetsCount <= bookingProperties.getMaxConcurrentBookings()) {
                slots++;
            }
        }

        return slots;
    }

    private boolean overlapsAvailabilityBlock(
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            List<AvailabilityBlock> availabilityBlocks
    ) {
        return availabilityBlocks.stream()
                .anyMatch(block -> block.getStartsAt().isBefore(endsAt) && block.getEndsAt().isAfter(startsAt));
    }

    private int usedHelmets(LocalDateTime startsAt, LocalDateTime endsAt, List<Booking> bookings) {
        return bookings.stream()
                .filter(booking -> booking.getStartsAt().isBefore(endsAt) && booking.getEndsAt().isAfter(startsAt))
                .mapToInt(Booking::getHelmetsCount)
                .sum();
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
                VrService.CURRENCY,
                service.isActive()
        );
    }

    private BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getService().getSlug(),
                b.getService().getTitle(),
                b.getService().getDurationMinutes(),
                b.getHelmetsCount(),
                bookingPrice(b),
                VrService.CURRENCY,
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
                b.getEndsAt(),
                b.getHelmetsCount()
        );
    }

    private AdminBookingResponse toAdminResponse(Booking b) {
        return new AdminBookingResponse(
                b.getId(),
                b.getService().getSlug(),
                b.getService().getTitle(),
                b.getService().getDurationMinutes(),
                b.getHelmetsCount(),
                bookingPrice(b),
                VrService.CURRENCY,
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

    private BigDecimal bookingPrice(Booking booking) {
        BigDecimal hourlyPrice = isWeekend(booking.getStartsAt().toLocalDate())
                ? WEEKEND_HOURLY_PRICE
                : WEEKDAY_HOURLY_PRICE;

        return hourlyPrice
                .multiply(BigDecimal.valueOf(booking.getService().getDurationMinutes()))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
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

    private String formatTime(LocalTime time) {
        return time.format(TIME_FORMATTER);
    }

    private int toMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
