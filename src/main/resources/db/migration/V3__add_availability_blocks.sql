CREATE TABLE IF NOT EXISTS availability_blocks (
    id BIGSERIAL PRIMARY KEY,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_availability_blocks_period CHECK (ends_at > starts_at)
);

CREATE INDEX IF NOT EXISTS idx_availability_blocks_period ON availability_blocks(starts_at, ends_at);
