package ua.com.virtum.booking.notification;

import org.junit.jupiter.api.Test;
import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.dto.PaymentProofResponse;
import ua.com.virtum.booking.entity.BookingStatus;
import ua.com.virtum.booking.entity.PaymentMethod;
import ua.com.virtum.booking.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class BookingNotificationServiceTests {
    @Test
    void keepsBookingFlowAliveWhenNotifierFails() {
        BookingNotifier failingNotifier = new BookingNotifier() {
            @Override
            public void bookingCreated(AdminBookingResponse booking) {
                throw new IllegalStateException("Telegram is unavailable");
            }

            @Override
            public void bookingStatusChanged(AdminBookingResponse booking, BookingStatus previousStatus) {
                throw new IllegalStateException("Telegram is unavailable");
            }
        };

        BookingNotificationService service = new BookingNotificationService(List.of(failingNotifier));

        assertThatCode(() -> service.bookingCreated(booking()))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.bookingStatusChanged(booking(), BookingStatus.CONFIRMED))
                .doesNotThrowAnyException();
    }

    @Test
    void forwardsEventsToConfiguredNotifiers() {
        RecordingNotifier notifier = new RecordingNotifier();
        BookingNotificationService service = new BookingNotificationService(List.of(notifier));
        AdminBookingResponse booking = booking();

        service.bookingCreated(booking);
        service.bookingStatusChanged(booking, BookingStatus.CONFIRMED);

        assertThat(notifier.events).containsExactly(
                "created:42",
                "status:42:CONFIRMED:CANCELLED"
        );
    }

    private AdminBookingResponse booking() {
        return new AdminBookingResponse(
                42L,
                "vr-party-60",
                "VR-вечірка 60 хв",
                60,
                new BigDecimal("1200.00"),
                "UAH",
                "Тестовий клієнт",
                "+380501234567",
                "test@example.com",
                LocalDateTime.of(2026, 7, 1, 14, 0),
                LocalDateTime.of(2026, 7, 1, 15, 0),
                BookingStatus.CANCELLED,
                PaymentMethod.PAY_AT_CLUB,
                PaymentStatus.UNPAID,
                new PaymentProofResponse(false, null, null, null, null),
                LocalDateTime.of(2026, 6, 28, 10, 0)
        );
    }

    private static class RecordingNotifier implements BookingNotifier {
        private final List<String> events = new ArrayList<>();

        @Override
        public void bookingCreated(AdminBookingResponse booking) {
            events.add("created:" + booking.id());
        }

        @Override
        public void bookingStatusChanged(AdminBookingResponse booking, BookingStatus previousStatus) {
            events.add("status:" + booking.id() + ":" + previousStatus + ":" + booking.status());
        }
    }
}
