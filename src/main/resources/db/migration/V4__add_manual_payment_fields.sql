ALTER TABLE bookings
    ADD COLUMN payment_method VARCHAR(32) NOT NULL DEFAULT 'PAY_AT_CLUB';

ALTER TABLE bookings
    ADD COLUMN payment_status VARCHAR(32) NOT NULL DEFAULT 'UNPAID';

ALTER TABLE bookings
    ADD COLUMN payment_upload_token VARCHAR(64);

ALTER TABLE bookings
    ADD COLUMN payment_proof_path VARCHAR(500);

ALTER TABLE bookings
    ADD COLUMN payment_proof_original_filename VARCHAR(255);

ALTER TABLE bookings
    ADD COLUMN payment_proof_content_type VARCHAR(100);

ALTER TABLE bookings
    ADD COLUMN payment_proof_uploaded_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_bookings_payment_status ON bookings(payment_status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_bookings_payment_upload_token ON bookings(payment_upload_token);
