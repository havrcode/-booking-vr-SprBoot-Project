package ua.com.virtum.booking.service;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ua.com.virtum.booking.config.PaymentProperties;
import ua.com.virtum.booking.entity.Booking;
import ua.com.virtum.booking.exception.BadRequestException;
import ua.com.virtum.booking.exception.NotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentProofStorage {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private final PaymentProperties paymentProperties;

    public PaymentProofStorage(PaymentProperties paymentProperties) {
        this.paymentProperties = paymentProperties;
    }

    public StoredPaymentProof store(Booking booking, MultipartFile file) {
        validate(file);

        try {
            Path root = root();
            Files.createDirectories(root);
            String filename = "booking-%d-%s.%s".formatted(
                    booking.getId(),
                    UUID.randomUUID(),
                    extension(file)
            );
            Path target = root.resolve(filename).normalize();

            if (!target.startsWith(root)) {
                throw new BadRequestException("Invalid payment proof file path.");
            }

            file.transferTo(target);
            return new StoredPaymentProof(
                    filename,
                    safeOriginalFilename(file.getOriginalFilename()),
                    file.getContentType()
            );
        } catch (IOException ex) {
            throw new BadRequestException("Could not save payment proof file.");
        }
    }

    public Resource load(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new NotFoundException("Payment proof not found.");
        }

        Path root = root();
        Path path = root.resolve(filename).normalize();

        if (!path.startsWith(root) || !Files.exists(path)) {
            throw new NotFoundException("Payment proof not found.");
        }

        return new FileSystemResource(path);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Payment proof file is required.");
        }

        if (file.getSize() > paymentProperties.getMaxProofSizeBytes()) {
            throw new BadRequestException("Payment proof file is too large.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Payment proof must be an image file.");
        }
    }

    private Path root() {
        return Path.of(paymentProperties.getProofsDir()).toAbsolutePath().normalize();
    }

    private String extension(MultipartFile file) {
        String contentType = String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT);

        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/heic" -> "heic";
            case "image/heif" -> "heif";
            default -> "img";
        };
    }

    private String safeOriginalFilename(String value) {
        if (value == null || value.isBlank()) {
            return "payment-proof";
        }

        return Path.of(value).getFileName().toString();
    }

    public record StoredPaymentProof(
            String filename,
            String originalFilename,
            String contentType
    ) {}
}
