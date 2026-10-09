package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShortenUrlRequest {

    @NotBlank(message = "URL must not be blank")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Pattern(
        regexp = "^https?://.*",
        message = "URL must start with http:// or https://"
    )
    private String originalUrl;

    @Pattern(
        regexp = "^[a-zA-Z0-9-]{3,50}$",
        message = "Custom alias must be 3-50 alphanumeric characters or hyphens"
    )
    private String customAlias;

    private LocalDateTime expiresAt;
}
