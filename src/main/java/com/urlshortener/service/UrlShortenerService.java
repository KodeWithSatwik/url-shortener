package com.urlshortener.service;

import com.urlshortener.config.AppConfig;
import com.urlshortener.dto.ShortenUrlRequest;
import com.urlshortener.dto.ShortenUrlResponse;
import com.urlshortener.exception.AliasAlreadyTakenException;
import com.urlshortener.exception.ShortCodeNotFoundException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.model.ShortenedUrl;
import com.urlshortener.repository.ShortenedUrlRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final ShortenedUrlRepository repository;
    private final Base62Encoder encoder;
    private final AppConfig appConfig;
    private final EntityManager entityManager;

    /**
     * Shorten a URL. If the same original URL already exists, return the existing entry (idempotent).
     * If a custom alias is provided, use it (or throw if taken).
     */
    @Transactional
    public ShortenUrlResponse shorten(ShortenUrlRequest request) {
        // Idempotency: return existing short code for the same URL (only if no custom alias)
        if (request.getCustomAlias() == null) {
            Optional<ShortenedUrl> existing = repository.findByOriginalUrlAndDeletedFalse(request.getOriginalUrl());
            if (existing.isPresent() && !existing.get().isExpired()) {
                return toResponse(existing.get()).withExisting(true);
            }
        }

        // Determine short code
        String shortCode;
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            shortCode = request.getCustomAlias();
            if (repository.existsByShortCode(shortCode)) {
                throw new AliasAlreadyTakenException(shortCode);
            }
        } else {
            shortCode = generateUniqueCode();
        }

        // Determine expiry
        LocalDateTime expiresAt = request.getExpiresAt() != null
                ? request.getExpiresAt()
                : LocalDateTime.now().plusDays(appConfig.getDefaultTtlDays());

        ShortenedUrl entity = ShortenedUrl.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .expiresAt(expiresAt)
                .build();

        ShortenedUrl saved = repository.save(entity);
        log.debug("Shortened URL: {} -> {}", shortCode, request.getOriginalUrl());
        return toResponse(saved);
    }

    /**
     * Resolve a short code to its original URL. Throws if not found or expired.
     */
    @Transactional(readOnly = true)
    public String resolve(String shortCode) {
        ShortenedUrl url = repository.findByShortCodeAndDeletedFalse(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));

        if (url.isExpired()) {
            throw new UrlExpiredException(shortCode);
        }

        return url.getOriginalUrl();
    }

    /**
     * Soft-delete a shortened URL.
     */
    @Transactional
    public void delete(String shortCode) {
        ShortenedUrl url = repository.findByShortCodeAndDeletedFalse(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
        url.setDeleted(true);
        repository.save(url);
        log.debug("Deleted short code: {}", shortCode);
    }

    /**
     * Scheduled cleanup: hard-delete rows that are soft-deleted or expired.
     *
     * Runs every 24 hours (configurable via app.cleanup.cron).
     * The FK ON DELETE CASCADE on click_events ensures no orphan rows are left behind.
     * This prevents unbounded table growth in production.
     */
    @Scheduled(cron = "${app.cleanup.cron:0 0 3 * * *}")  // default: 03:00 every night
    @Transactional
    public void purgeExpiredAndDeleted() {
        int removed = repository.deleteExpiredAndDeleted(LocalDateTime.now());
        if (removed > 0) {
            log.info("Cleanup job: purged {} expired/deleted URL(s)", removed);
        }
    }

    // --- private helpers ---

    /**
     * Draw the next value from the DB sequence and encode it as a Base62 short code.
     * On the extremely rare collision (manual inserts reusing a code), retry with offset.
     */
    private String generateUniqueCode() {
        int maxRetries = appConfig.getShortCode().getMaxRetries();
        // nextval() works in both H2 2.x and PostgreSQL (ANSI-compatible sequence syntax)
        Long seqVal = ((Number) entityManager
                .createNativeQuery("SELECT nextval('short_code_seq')")
                .getSingleResult()).longValue();
        String code = encoder.encode(seqVal);
        int attempt = 0;
        while (repository.existsByShortCode(code) && attempt < maxRetries) {
            seqVal = ((Number) entityManager
                    .createNativeQuery("SELECT nextval('short_code_seq')")
                    .getSingleResult()).longValue();
            code = encoder.encode(seqVal);
            attempt++;
        }
        if (attempt >= maxRetries) {
            throw new IllegalStateException("Could not generate a unique short code after " + maxRetries + " attempts");
        }
        return code;
    }

    private ShortenUrlResponse toResponse(ShortenedUrl url) {
        return ShortenUrlResponse.builder()
                .shortCode(url.getShortCode())
                .shortUrl(appConfig.getBaseUrl() + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .expiresAt(url.getExpiresAt())
                .createdAt(url.getCreatedAt())
                .build();
    }
}
