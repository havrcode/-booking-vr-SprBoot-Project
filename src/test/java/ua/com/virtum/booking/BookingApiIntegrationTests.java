package ua.com.virtum.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsSeededServices() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andExpect(jsonPath("$[0].slug").exists())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    void createsBookingAndRejectsOverlappingSlot() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(7)
                .withHour(14)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String payload = """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Test Customer",
                  "customerPhone": "+380501234567",
                  "customerEmail": "test@example.com",
                  "startsAt": "%s"
                }
                """.formatted(startsAtValue);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceSlug").value("vr-party-60"))
                .andExpect(jsonPath("$.startsAt").value(startsAtValue));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
