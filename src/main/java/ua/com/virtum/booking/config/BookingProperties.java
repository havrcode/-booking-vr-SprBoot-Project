package ua.com.virtum.booking.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.booking")
public class BookingProperties {
    @Min(1)
    private int maxConcurrentBookings = 1;

    public int getMaxConcurrentBookings() {
        return maxConcurrentBookings;
    }

    public void setMaxConcurrentBookings(int maxConcurrentBookings) {
        this.maxConcurrentBookings = maxConcurrentBookings;
    }
}
