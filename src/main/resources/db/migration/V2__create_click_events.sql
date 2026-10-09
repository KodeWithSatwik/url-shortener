-- V2: Create click_events table with FK to shortened_urls
-- Compatible with H2 (dev/test) and PostgreSQL (prod)
CREATE TABLE click_events (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_code VARCHAR(50) NOT NULL,
    clicked_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    referrer   VARCHAR(2048),
    ip_address VARCHAR(45),
    CONSTRAINT fk_click_short_code
        FOREIGN KEY (short_code) REFERENCES shortened_urls (short_code)
        ON DELETE CASCADE
);

CREATE INDEX idx_click_events_short_code ON click_events (short_code);
CREATE INDEX idx_click_events_clicked_at ON click_events (clicked_at);
