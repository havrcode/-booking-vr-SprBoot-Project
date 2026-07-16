package ua.com.virtum.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookingVrApplicationTests {
    @Autowired
    private Clock clock;

    @Test
    void contextLoads() {}

    @Test
    void usesKyivApplicationClock() {
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Europe/Kyiv"));
    }
}
