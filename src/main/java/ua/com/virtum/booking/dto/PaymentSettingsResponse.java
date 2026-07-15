package ua.com.virtum.booking.dto;

public record PaymentSettingsResponse(
        boolean payAtClubEnabled,
        boolean cardTransferEnabled,
        String cardHolder,
        String cardNumber,
        String cardBank,
        String cardTransferNote,
        long maxProofSizeBytes
) {}
