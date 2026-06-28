package ua.com.virtum.booking.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.entity.BookingStatus;

import java.util.List;

@Service
public class BookingNotificationService {
    private static final Logger log = LoggerFactory.getLogger(BookingNotificationService.class);

    private final List<BookingNotifier> notifiers;

    public BookingNotificationService(List<BookingNotifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void bookingCreated(AdminBookingResponse booking) {
        for (BookingNotifier notifier : notifiers) {
            try {
                notifier.bookingCreated(booking);
            } catch (RuntimeException ex) {
                // Notification delivery must not break the booking workflow.
                // The booking is more important than a secondary delivery channel.
                log.warn("Booking notification failed for booking {}", booking.id(), ex);
            }
        }
    }

    public void bookingStatusChanged(AdminBookingResponse booking, BookingStatus previousStatus) {
        for (BookingNotifier notifier : notifiers) {
            try {
                notifier.bookingStatusChanged(booking, previousStatus);
            } catch (RuntimeException ex) {
                log.warn("Booking status notification failed for booking {}", booking.id(), ex);
            }
        }
    }
}

