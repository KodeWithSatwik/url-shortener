# Phase 2 — Implementation (Core URL Shortener Service)

**Status**: PENDING
**Gate Entry**: Human approved architecture + API contracts from Phase 1.
**Gate Exit**: Human reviews implementation, code quality, and API behaviour.

---

## Sub-Tasks

### 2.1 Project Scaffold
- [ ] `pom.xml` — Spring Boot 3, Spring Web, Spring Data JPA, Jakarta Validation, H2, Flyway, SpringDoc, Actuator, Lombok
- [ ] `UrlShortenerApplication.java` — main class
- [ ] `application.yml` — dev config (H2, port, base URL)
- [ ] `application-prod.yml` — prod config (PostgreSQL)

### 2.2 Database Layer
- [ ] `V1__create_shortened_urls.sql` — Flyway migration for ShortenedUrl table
- [ ] `V2__create_click_events.sql` — Flyway migration for ClickEvent table
- [ ] `ShortenedUrl.java` — JPA entity (`@Entity`, Jakarta Persistence annotations)
- [ ] `ClickEvent.java` — JPA entity
- [ ] `ShortenedUrlRepository.java` — Spring Data JPA repo (findByShortCode, existsByShortCode)
- [ ] `ClickEventRepository.java` — Spring Data JPA repo

### 2.3 Business Logic — URL Shortening
- [ ] `Base62Encoder.java` — encode long ID to Base62 short code (a-z, A-Z, 0-9)
- [ ] `UrlShortenerService.java` — shorten(originalUrl): generate code, handle collision, persist
- [ ] Collision handling strategy: retry with incremented counter (max 5 retries)
- [ ] `UrlShortenerService.java` — resolve(shortCode): lookup, check expiry, return original URL
- [ ] `UrlShortenerService.java` — delete(shortCode): soft delete or hard delete

### 2.4 Analytics
- [ ] `AnalyticsService.java` — recordClick(shortCode, referrer, ip): persist ClickEvent
- [ ] `AnalyticsService.java` — getStats(shortCode): return click count, top referrers, recent clicks
- [ ] Analytics recorded asynchronously to not block redirect response

### 2.5 REST Controllers
- [ ] `UrlController.java` — POST /api/v1/urls → ShortenUrlRequest → ShortenUrlResponse
- [ ] `UrlController.java` — GET /{shortCode} → HTTP 302 redirect
- [ ] `UrlController.java` — DELETE /api/v1/urls/{shortCode} → 204 No Content
- [ ] `AnalyticsController.java` — GET /api/v1/urls/{shortCode}/analytics → AnalyticsResponse

### 2.6 DTOs
- [ ] `ShortenUrlRequest.java` — `{ originalUrl, customAlias?, expiresAt? }` with Jakarta Validation
- [ ] `ShortenUrlResponse.java` — `{ shortCode, shortUrl, originalUrl, expiresAt, createdAt }`
- [ ] `AnalyticsResponse.java` — `{ shortCode, totalClicks, recentClicks[], topReferrers[] }`
- [ ] `ErrorResponse.java` — `{ timestamp, status, error, message, path }`

### 2.7 Exception Handling
- [ ] `ShortCodeNotFoundException.java`
- [ ] `UrlExpiredException.java`
- [ ] `InvalidUrlException.java`
- [ ] `GlobalExceptionHandler.java` — `@RestControllerAdvice` handling all above + validation errors

### 2.8 Configuration
- [ ] `AppConfig.java` — base URL, default TTL, max retries
- [ ] `RateLimitConfig.java` — basic in-memory rate limiter (Bucket4j or simple counter)
- [ ] `AsyncConfig.java` — thread pool for async analytics

### 2.9 Reliability Features
- [ ] Input validation (URL format check, max length)
- [ ] Expiry check on redirect
- [ ] Idempotency: same long URL → return existing short code (configurable)

---

## Outputs
- Full Spring Boot application under `src/main/java/com/urlshortener/`
- Database migrations under `src/main/resources/db/migration/`
- `application.yml` and `application-prod.yml`
