package ua.com.virtum.booking.notification;

import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.entity.BookingStatus;

public interface BookingNotifier {
    void bookingCreated(AdminBookingResponse booking);

    void bookingStatusChanged(AdminBookingResponse booking, BookingStatus previousStatus);
}

