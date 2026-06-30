package ua.com.virtum.booking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TelegramNotificationProperties.class)
public class NotificationConfig {
}

