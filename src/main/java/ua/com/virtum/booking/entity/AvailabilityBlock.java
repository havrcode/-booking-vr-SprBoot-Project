package ua.com.virtum.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "availability_blocks", indexes = @Index(name = "idx_availability_blocks_period", columnList = "starts_at, ends_at"))
public class AvailabilityBlock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDateTime startsAt;
    @Column(nullable = false)
    private LocalDateTime endsAt;
    @Column(length = 255)
    private String reason;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
