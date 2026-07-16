package ua.com.virtum.booking;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Testcontainers(disabledWithoutDocker = true)
class PostgresProfileIntegrationTests {
    private static final String ADMIN_API_KEY = "postgres-test-admin-key";

    // Testcontainers starts this PostgreSQL database only when Docker is available.
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("virtum_booking_test")
            .withUsername("virtum_booking")
            .withPassword("test-password");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        // Keep the prod profile active, but point it to the disposable test database.
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.admin.api-key", () -> ADMIN_API_KEY);
        registry.add("app.notifications.telegram.enabled", () -> false);
    }

    @Test
    void prodProfileRunsFlywayAndBookingFlowAgainstPostgres() throws Exception {
        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));

        // Use a future slot so the test stays valid no matter when the suite runs.
        LocalDateTime startsAt = LocalDateTime.now()
                .plusDays(14)
                .withHour(12)
                .withMinute(30)
                .withSecond(0)
                .withNano(0);
        String startsAtValue = startsAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String dateValue = startsAt.toLocalDate().toString();

        String payload = """
                {
                  "serviceSlug": "vr-party-60",
                  "customerName": "Тестовий клієнт Postgres",
                  "customerPhone": "+380501234569",
                  "customerEmail": "postgres-test@example.com",
                  "startsAt": "%s"
                }
                """.formatted(startsAtValue);

        MvcResult created = mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn();
        Integer bookingId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .header("X-Admin-Api-Key", ADMIN_API_KEY)
                        .param("from", dateValue)
                        .param("to", dateValue))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(bookingId));

        mockMvc.perform(patch("/api/v1/admin/bookings/{id}/status", bookingId)
                        .header("X-Admin-Api-Key", ADMIN_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
