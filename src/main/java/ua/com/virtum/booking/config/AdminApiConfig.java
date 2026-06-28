package ua.com.virtum.booking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminApiConfig implements WebMvcConfigurer {
    private final AdminProperties adminProperties;

    public AdminApiConfig(AdminProperties adminProperties) {
        this.adminProperties = adminProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminApiKeyInterceptor(adminProperties))
                .addPathPatterns("/api/v1/admin/**");
    }
}

