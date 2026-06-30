package ua.com.virtum.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.com.virtum.booking.entity.VrService;

import java.util.List;
import java.util.Optional;

public interface VrServiceRepository extends JpaRepository<VrService, Long> {
    List<VrService> findByActiveTrueOrderByTitleAsc();
    List<VrService> findAllByOrderByActiveDescTitleAsc();
    Optional<VrService> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
}
