package ua.com.virtum.booking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Instant;

public class AdminApiKeyInterceptor implements HandlerInterceptor {
    public static final String ADMIN_API_KEY_HEADER = "X-Admin-Api-Key";

    private final AdminProperties adminProperties;

    public AdminApiKeyInterceptor(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // Browser preflight requests do not carry custom API-key headers consistently.
        // Real admin requests are still checked below.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String expectedApiKey = adminProperties.getApiKey();
        String actualApiKey = request.getHeader(ADMIN_API_KEY_HEADER);

        if (expectedApiKey != null && !expectedApiKey.isBlank() && expectedApiKey.equals(actualApiKey)) {
            return true;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"status":401,"error":"Admin API key is missing or invalid.","timestamp":"%s"}
                """.formatted(Instant.now()));
        return false;
    }
}
