ALTER TABLE bookings
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED';

CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);

