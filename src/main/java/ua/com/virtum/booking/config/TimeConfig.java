package ua.com.virtum.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {
    @Bean
    Clock applicationClock(TimeProperties timeProperties) {
        return Clock.system(ZoneId.of(timeProperties.getZoneId()));
    }
}
