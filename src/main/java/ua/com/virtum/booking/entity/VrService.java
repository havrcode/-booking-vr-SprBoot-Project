package ua.com.virtum.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vr_services")
public class VrService {
    public static final String CURRENCY = "UAH";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String slug;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false)
    private Integer durationMinutes;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    private boolean active = true;

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    @Transient
    public String getCurrency() { return CURRENCY; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
