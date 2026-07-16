package ua.com.virtum.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings", indexes = @Index(name = "idx_booking_starts_at", columnList = "starts_at"))
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private VrService service;
    @Column(nullable = false, length = 120)
    private String customerName;
    @Column(nullable = false, length = 32)
    private String customerPhone;
    @Column(nullable = false, length = 255)
    private String customerEmail;
    @Column(nullable = false)
    private LocalDateTime startsAt;
    @Column(nullable = false)
    private LocalDateTime endsAt;
    @Column(nullable = false)
    private int helmetsCount = 1;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status = BookingStatus.CONFIRMED;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentMethod paymentMethod = PaymentMethod.PAY_AT_CLUB;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    @Column(length = 64)
    private String paymentUploadToken;
    @Column(length = 500)
    private String paymentProofPath;
    @Column(length = 255)
    private String paymentProofOriginalFilename;
    @Column(length = 100)
    private String paymentProofContentType;
    private LocalDateTime paymentProofUploadedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public VrService getService() { return service; }
    public void setService(VrService service) { this.service = service; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public int getHelmetsCount() { return helmetsCount; }
    public void setHelmetsCount(int helmetsCount) { this.helmetsCount = helmetsCount; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentUploadToken() { return paymentUploadToken; }
    public void setPaymentUploadToken(String paymentUploadToken) { this.paymentUploadToken = paymentUploadToken; }
    public String getPaymentProofPath() { return paymentProofPath; }
    public void setPaymentProofPath(String paymentProofPath) { this.paymentProofPath = paymentProofPath; }
    public String getPaymentProofOriginalFilename() { return paymentProofOriginalFilename; }
    public void setPaymentProofOriginalFilename(String paymentProofOriginalFilename) { this.paymentProofOriginalFilename = paymentProofOriginalFilename; }
    public String getPaymentProofContentType() { return paymentProofContentType; }
    public void setPaymentProofContentType(String paymentProofContentType) { this.paymentProofContentType = paymentProofContentType; }
    public LocalDateTime getPaymentProofUploadedAt() { return paymentProofUploadedAt; }
    public void setPaymentProofUploadedAt(LocalDateTime paymentProofUploadedAt) { this.paymentProofUploadedAt = paymentProofUploadedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
