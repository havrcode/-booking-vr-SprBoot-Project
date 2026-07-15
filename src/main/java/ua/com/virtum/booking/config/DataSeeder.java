package ua.com.virtum.booking.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.com.virtum.booking.entity.VrService;
import ua.com.virtum.booking.repository.VrServiceRepository;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(VrServiceRepository repository) {
        return args -> {
            if (repository.count() > 0) return;
            repository.save(service("vr-party-60", "VR-вечірка 60 хв", 60, "1200"));
            repository.save(service("vr-quest-90", "VR-квест 90 хв", 90, "1700"));
            repository.save(service("vr-kids-45", "VR для дітей 45 хв", 45, "900"));
        };
    }

    private VrService service(String slug, String title, int minutes, String price) {
        VrService s = new VrService();
        s.setSlug(slug);
        s.setTitle(title);
        s.setDurationMinutes(minutes);
        s.setPrice(new BigDecimal(price));
        s.setActive(true);
        return s;
    }
}
