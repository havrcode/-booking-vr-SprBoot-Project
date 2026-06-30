package ua.com.virtum.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications.telegram")
public class TelegramNotificationProperties {
    private boolean enabled;
    private String botToken;
    private String chatId;
    private String baseUrl = "https://api.telegram.org";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isConfigured() {
        return enabled
                && botToken != null
                && !botToken.isBlank()
                && chatId != null
                && !chatId.isBlank();
    }
}

