package ua.com.virtum.booking.notification;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ua.com.virtum.booking.config.TelegramNotificationProperties;
import ua.com.virtum.booking.dto.AdminBookingResponse;
import ua.com.virtum.booking.entity.BookingStatus;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class TelegramBookingNotifier implements BookingNotifier {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TelegramNotificationProperties properties;
    private final RestClient restClient;

    public TelegramBookingNotifier(TelegramNotificationProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public void bookingCreated(AdminBookingResponse booking) {
        send("""
                <b>Нове бронювання</b>
                %s
                """.formatted(bookingDetails(booking)));
    }

    @Override
    public void bookingStatusChanged(AdminBookingResponse booking, BookingStatus previousStatus) {
        send("""
                <b>Статус бронювання змінено</b>
                Було: %s
                Стало: %s

                %s
                """.formatted(previousStatus, booking.status(), bookingDetails(booking)));
    }

    private void send(String text) {
        if (!properties.isConfigured()) {
            return;
        }

        restClient.post()
                .uri("/bot{botToken}/sendMessage", properties.getBotToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "chat_id", properties.getChatId(),
                        "text", text,
                        "parse_mode", "HTML",
                        "disable_web_page_preview", true
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private String bookingDetails(AdminBookingResponse booking) {
        return """
                ID: #%s
                Послуга: %s
                Клієнт: %s
                Телефон: %s
                Email: %s
                Дата: %s
                Час: %s-%s
                Статус: %s
                """.formatted(
                booking.id(),
                html(booking.serviceTitle()),
                html(booking.customerName()),
                html(booking.customerPhone()),
                html(booking.customerEmail()),
                booking.startsAt().format(DATE_FORMATTER),
                booking.startsAt().format(TIME_FORMATTER),
                booking.endsAt().format(TIME_FORMATTER),
                booking.status()
        );
    }

    private String html(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

