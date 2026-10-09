package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShortenUrlResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    /** True when an existing active record was returned (idempotent dedup). Not serialised to JSON. */
    @JsonIgnore
    @Builder.Default
    private boolean existing = false;

    public ShortenUrlResponse withExisting(boolean value) {
        this.existing = value;
        return this;
    }
}
