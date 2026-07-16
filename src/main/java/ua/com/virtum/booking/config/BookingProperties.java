package ua.com.virtum.booking.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;

@Validated
@ConfigurationProperties(prefix = "app.booking")
public class BookingProperties {
    @Min(1)
    private int maxConcurrentBookings = 2;

    @NotNull
    private LocalTime openTime = LocalTime.of(9, 30);

    @NotNull
    private LocalTime closeTime = LocalTime.of(20, 30);

    @NotNull
    private LocalTime breakStart = LocalTime.of(14, 30);

    @NotNull
    private LocalTime breakEnd = LocalTime.of(15, 30);

    @Min(1)
    private int slotStepMinutes = 60;

    public int getMaxConcurrentBookings() {
        return maxConcurrentBookings;
    }

    public void setMaxConcurrentBookings(int maxConcurrentBookings) {
        this.maxConcurrentBookings = maxConcurrentBookings;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    public LocalTime getBreakStart() {
        return breakStart;
    }

    public void setBreakStart(LocalTime breakStart) {
        this.breakStart = breakStart;
    }

    public LocalTime getBreakEnd() {
        return breakEnd;
    }

    public void setBreakEnd(LocalTime breakEnd) {
        this.breakEnd = breakEnd;
    }

    public int getSlotStepMinutes() {
        return slotStepMinutes;
    }

    public void setSlotStepMinutes(int slotStepMinutes) {
        this.slotStepMinutes = slotStepMinutes;
    }
}
