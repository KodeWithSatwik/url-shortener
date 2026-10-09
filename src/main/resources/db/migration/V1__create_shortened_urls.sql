-- V1: Create shortened_urls table
-- Compatible with H2 (dev/test) and PostgreSQL (prod)
CREATE TABLE shortened_urls (
    id           BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_code   VARCHAR(50)  NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP,
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_shortened_urls_short_code   ON shortened_urls (short_code);
CREATE INDEX idx_shortened_urls_original_url ON shortened_urls (original_url);
