package ua.com.virtum.booking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.payment.card-transfer-enabled=true",
        "app.payment.card-holder=Virtum VR",
        "app.payment.card-number=4444555566667777",
        "app.payment.card-bank=Тестовий банк",
        "app.payment.proofs-dir=target/payment-proofs-test"
})
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
                .andExpect(jsonPath("$[0].title").exists())
                .andExpect(jsonPath("$[?(@.slug == 'vr-arena-120')]", hasSize(1)))
                .andExpect(jsonPath("$[0].currency").value("UAH"));
    }

    @Test
    void exposesPublicBookingSettings() throws Exception {
        mockMvc.perform(get("/api/v1/booking-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxConcurrentBookings").value(2))
                .andExpect(jsonPath("$.schedule.openTime").value("09:30"))
                .andExpect(jsonPath("$.schedule.closeTime").value("20:30"))
                .andExpect(jsonPath("$.schedule.breakStart").value("14:30"))
                .andExpect(jsonPath("$.schedule.breakEnd").value("15:30"))
                .andExpect(jsonPath("$.schedule.slotStepMinutes").value(60))
                .andExpect(jsonPath("$.payment.payAtClubEnabled").value(true))
                .andExpect(jsonPath("$.payment.cardTransferEnabled").value(true))
                .andExpect(jsonPath("$.payment.cardNumber").value("4444555566667777"));
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
                .withHour(10)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String payload = """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Тестовий клієнт",
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
                .andExpect(jsonPath("$.currency").value("UAH"))
                .andExpect(jsonPath("$.helmetsCount").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentMethod").value("PAY_AT_CLUB"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));

        mockMvc.perform(get("/api/v1/bookings")
                        .param("date", startsAt.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startsAt").value(startsAtValue))
                .andExpect(jsonPath("$[0].endsAt").exists())
                .andExpect(jsonPath("$[0].helmetsCount").value(1))
                .andExpect(jsonPath("$[0].customerName").doesNotExist())
                .andExpect(jsonPath("$[0].customerPhone").doesNotExist())
                .andExpect(jsonPath("$[0].customerEmail").doesNotExist());

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void exposesDayAvailabilityForCalendar() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(18)
                .withHour(9)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        String dateValue = startsAt.toLocalDate().toString();

        mockMvc.perform(get("/api/v1/booking-days")
                        .param("from", dateValue)
                        .param("to", dateValue)
                        .param("serviceSlug", "vr-party-60")
                        .param("helmetsCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value(dateValue))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[0].availableSlots").exists());
    }

    @Test
    void rejectsLunchBreakAndClosedHours() throws Exception {
        LocalDateTime lunchStartsAt = LocalDateTime.now()
                .plusDays(17)
                .withHour(14)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        LocalDateTime lateStartsAt = lunchStartsAt
                .plusDays(1)
                .withHour(20)
                .withMinute(30);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(lunchStartsAt, "lunch-break@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(lateStartsAt, "after-close@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void adminCanListAndCancelBooking() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(8)
                .withHour(16)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String dateValue = startsAt.toLocalDate().toString();

        String payload = """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Тестовий клієнт адмінки",
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
                .andExpect(jsonPath("$[0].currency").value("UAH"))
                .andExpect(jsonPath("$[0].helmetsCount").value(1))
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
    void clientCanUploadPaymentProofAndAdminCanMarkPaid() throws Exception {
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(12)
                .withHour(11)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String dateValue = startsAt.toLocalDate().toString();

        String payload = """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Клієнт з переказом на карту",
                  "customerPhone": "+380501234571",
                  "customerEmail": "card-transfer@example.com",
                  "startsAt": "%s",
                  "paymentMethod": "CARD_TRANSFER"
                }
                """.formatted(startsAtValue);

        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CARD_TRANSFER"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.paymentUploadToken").exists())
                .andReturn();
        Integer bookingId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        String paymentUploadToken = JsonPath.read(created.getResponse().getContentAsString(), "$.paymentUploadToken");

        MockMultipartFile proof = new MockMultipartFile(
                "file",
                "payment-proof.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        mockMvc.perform(multipart("/api/v1/bookings/{id}/payment-proof", bookingId)
                        .file(proof)
                        .param("token", paymentUploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING_REVIEW"));

        mockMvc.perform(get("/api/v1/admin/bookings/{id}/payment-proof", bookingId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/bookings/{id}/payment-proof", bookingId)
                        .header("X-Admin-Api-Key", "dev-admin-key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .param("from", dateValue)
                        .param("to", dateValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId))
                .andExpect(jsonPath("$[0].paymentStatus").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$[0].paymentProof.uploaded").value(true))
                .andExpect(jsonPath("$[0].paymentProof.url").value("/api/v1/admin/bookings/" + bookingId + "/payment-proof"));

        mockMvc.perform(patch("/api/v1/admin/bookings/{id}/payment-status", bookingId)
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    void adminCanCreateUpdateAndDeactivateVrService() throws Exception {
        String createPayload = """
                {
                  "slug": "admin-managed-120",
                  "title": "Адмінська послуга 120 хв",
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
                .andExpect(jsonPath("$.currency").value("UAH"))
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
                  "title": "Адмінська послуга 75 хв",
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
                .andExpect(jsonPath("$.title").value("Адмінська послуга 75 хв"))
                .andExpect(jsonPath("$.durationMinutes").value(75))
                .andExpect(jsonPath("$.currency").value("UAH"))
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
                .withMinute(30)
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
                  "reason": "Приватна подія"
                }
                """.formatted(startsAtValue, endsAtValue);

        MvcResult createdBlock = mockMvc.perform(post("/api/v1/admin/availability-blocks")
                        .header("X-Admin-Api-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsAt").value(startsAtValue))
                .andExpect(jsonPath("$.reason").value("Приватна подія"))
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
                  "customerName": "Клієнт закритого слота",
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

    private String payload(LocalDateTime startsAt, String email) {
        return """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Клієнт перевірки графіка",
                  "customerPhone": "+380501234572",
                  "customerEmail": "%s",
                  "startsAt": "%s"
                }
                """.formatted(email, startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
}
