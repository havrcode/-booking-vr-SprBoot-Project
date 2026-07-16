ALTER TABLE bookings
    ALTER COLUMN customer_email DROP NOT NULL;

ALTER TABLE bookings
    ADD COLUMN customer_comment VARCHAR(500);
