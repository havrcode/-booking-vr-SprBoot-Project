ALTER TABLE bookings
    ADD COLUMN helmets_count INTEGER NOT NULL DEFAULT 1;

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_helmets_count CHECK (helmets_count > 0);
