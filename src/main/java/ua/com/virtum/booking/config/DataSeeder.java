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
            seedIfMissing(repository, "vr-party-60", "VR-вечірка 60 хв", 60, "1200");
            seedIfMissing(repository, "vr-arena-120", "VR-арена 120 хв", 120, "2200");
            seedIfMissing(repository, "vr-quest-90", "VR-квест 90 хв", 90, "1700");
            seedIfMissing(repository, "vr-kids-45", "VR для дітей 45 хв", 45, "900");
        };
    }

    private void seedIfMissing(VrServiceRepository repository, String slug, String title, int minutes, String price) {
        if (!repository.existsBySlug(slug)) {
            repository.save(service(slug, title, minutes, price));
        }
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
