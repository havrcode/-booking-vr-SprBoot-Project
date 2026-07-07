package ua.com.virtum.booking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void servesAdminPage() throws Exception {
        mockMvc.perform(get("/admin.html"))
                .andExpect(status().isOk());
    }

    @Test
    void servesPublicBookingWidgetAssets() throws Exception {
        mockMvc.perform(get("/widget/booking-widget.css"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/widget/booking-widget.js"))
                .andExpect(status().isOk());
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
                .andExpect(jsonPath("$.startsAt").value(startsAtValue))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void adminCanListAndCancelBooking() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(8)
                .withHour(16)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String dateValue = startsAt.toLocalDate().toString();

        String payload = """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Admin Test Customer",
                  "customerPhone": "+380501234568",
                  "customerEmail": "admin-test@example.com",
                  "startsAt": "%s"
                }
                """.formatted(startsAtValue);

        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        Integer bookingId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .param("from", dateValue)
                        .param("to", dateValue))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .param("from", dateValue)
                        .param("to", dateValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        mockMvc.perform(patch("/api/v1/admin/bookings/{id}/status", bookingId)
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void adminCanCreateUpdateAndDeactivateVrService() throws Exception {
        String createPayload = """
                {
                  "slug": "admin-managed-120",
                  "title": "Admin Managed 120 min",
                  "durationMinutes": 120,
                  "price": 2200.00,
                  "active": true
                }
                """;

        mockMvc.perform(get("/api/v1/admin/services"))
                .andExpect(status().isUnauthorized());

        MvcResult created = mockMvc.perform(post("/api/v1/admin/services")
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("admin-managed-120"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        Integer serviceId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/v1/admin/services")
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        String updatePayload = """
                {
                  "slug": "admin-managed-120",
                  "title": "Admin Managed 75 min",
                  "durationMinutes": 75,
                  "price": 1500.00,
                  "active": false
                }
                """;

        mockMvc.perform(put("/api/v1/admin/services/{id}", serviceId)
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin Managed 75 min"))
                .andExpect(jsonPath("$.durationMinutes").value(75))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == 'admin-managed-120')]", hasSize(0)));
    }

    @Test
    void adminCanCloseAndReopenAvailabilitySlot() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(9)
                .withHour(18)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime endsAt = startsAt.plusHours(1);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endsAtValue = endsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String dateValue = startsAt.toLocalDate().toString();

        String blockPayload = """
                {
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "reason": "Private event"
                }
                """.formatted(startsAtValue, endsAtValue);

        MvcResult createdBlock = mockMvc.perform(post("/api/v1/admin/availability-blocks")
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsAt").value(startsAtValue))
                .andExpect(jsonPath("$.reason").value("Private event"))
                .andReturn();
        Integer blockId = JsonPath.read(createdBlock.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/availability-blocks")
                        .param("date", dateValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockId))
                .andExpect(jsonPath("$[0].reason").doesNotExist());

        String bookingPayload = """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Blocked Slot Customer",
                  "customerPhone": "+380501234569",
                  "customerEmail": "blocked@example.com",
                  "startsAt": "%s"
                }
                """.formatted(startsAtValue);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(delete("/api/v1/admin/availability-blocks/{id}", blockId)
                        .header("X-Admin-Api-Key", "dev-admin-key"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
