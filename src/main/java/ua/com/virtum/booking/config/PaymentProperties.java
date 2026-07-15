package ua.com.virtum.booking.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {
    private boolean payAtClubEnabled = true;
    private boolean cardTransferEnabled = false;
    private String cardHolder = "";
    private String cardNumber = "";
    private String cardBank = "";
    private String cardTransferNote = "Після переказу можна додати скрін підтвердження.";
    private String proofsDir = "data/payment-proofs";
    @Min(1)
    private long maxProofSizeBytes = 8 * 1024 * 1024;

    public boolean isPayAtClubEnabled() {
        return payAtClubEnabled;
    }

    public void setPayAtClubEnabled(boolean payAtClubEnabled) {
        this.payAtClubEnabled = payAtClubEnabled;
    }

    public boolean isCardTransferEnabled() {
        return cardTransferEnabled;
    }

    public void setCardTransferEnabled(boolean cardTransferEnabled) {
        this.cardTransferEnabled = cardTransferEnabled;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardBank() {
        return cardBank;
    }

    public void setCardBank(String cardBank) {
        this.cardBank = cardBank;
    }

    public String getCardTransferNote() {
        return cardTransferNote;
    }

    public void setCardTransferNote(String cardTransferNote) {
        this.cardTransferNote = cardTransferNote;
    }

    public String getProofsDir() {
        return proofsDir;
    }

    public void setProofsDir(String proofsDir) {
        this.proofsDir = proofsDir;
    }

    public long getMaxProofSizeBytes() {
        return maxProofSizeBytes;
    }

    public void setMaxProofSizeBytes(long maxProofSizeBytes) {
        this.maxProofSizeBytes = maxProofSizeBytes;
    }
}
