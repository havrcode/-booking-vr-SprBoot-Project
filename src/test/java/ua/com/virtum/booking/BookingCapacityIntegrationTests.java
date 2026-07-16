package ua.com.virtum.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.booking.max-concurrent-bookings=2")
@AutoConfigureMockMvc
class BookingCapacityIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsBookingsUntilConfiguredCapacityIsFull() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(16)
                .withHour(13)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        mockMvc.perform(get("/api/v1/booking-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxConcurrentBookings").value(2));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(startsAtValue, "first@example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(startsAtValue, "second@example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(startsAtValue, "third@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void bookingTwoHelmetsConsumesWholeSlotCapacity() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(19)
                .withHour(10)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(startsAtValue, "two-helmets@example.com", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.helmetsCount").value(2));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(startsAtValue, "extra-helmet@example.com", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private String payload(String startsAtValue, String email) {
        return payload(startsAtValue, email, 1);
    }

    private String payload(String startsAtValue, String email, int helmetsCount) {
        return """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Клієнт для тесту місткості",
                  "customerPhone": "+380501234570",
                  "customerEmail": "%s",
                  "startsAt": "%s",
                  "helmetsCount": %d
                }
                """.formatted(email, startsAtValue, helmetsCount);
    }
}
