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
            seedIfMissing(repository, "vr-party-60", "VR-вечірка 60 хв", 60, "400");
            seedIfMissing(repository, "vr-sprint-120", "VR-спрінт 120 хв", 120, "800");
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
