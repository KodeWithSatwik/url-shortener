# Phase 3 — Testing

**Status**: PENDING
**Gate Entry**: Human approved implementation from Phase 2.
**Gate Exit**: Human reviews test results and coverage report.

---

## Sub-Tasks

### 3.1 Unit Tests — Core Logic
- [ ] `Base62EncoderTest.java` — encode/decode round-trip, edge cases (0, max long, negative)
- [ ] `UrlShortenerServiceTest.java` — shorten happy path, collision retry, expiry logic
- [ ] `AnalyticsServiceTest.java` — recordClick, getStats aggregation

### 3.2 Unit Tests — Controllers (MockMvc slice)
- [ ] `UrlControllerTest.java` — POST /api/v1/urls (valid, invalid URL, missing body)
- [ ] `UrlControllerTest.java` — GET /{shortCode} (found, not found, expired)
- [ ] `UrlControllerTest.java` — DELETE /api/v1/urls/{shortCode} (found, not found)
- [ ] `AnalyticsControllerTest.java` — GET /api/v1/urls/{shortCode}/analytics

### 3.3 Integration Tests — Full Stack
- [ ] `UrlShortenerIntegrationTest.java` — full POST→GET→redirect→analytics flow
- [ ] `UrlExpiryIntegrationTest.java` — create URL with expiry, advance clock, verify 410 Gone
- [ ] `CollisionHandlingIntegrationTest.java` — force collision, verify retry succeeds
- [ ] `RateLimitIntegrationTest.java` — exceed rate limit, verify 429 response

### 3.4 Edge Case Tests
- [ ] Invalid URL formats (javascript:, ftp://, empty string, > 2048 chars)
- [ ] Non-existent short code → 404
- [ ] Deleted short code → 404 or 410
- [ ] Analytics for unknown short code → 404
- [ ] Concurrent shorten requests (same long URL) → idempotency check

### 3.5 Test Infrastructure
- [ ] `@SpringBootTest` with H2 in-memory for integration tests
- [ ] Test fixtures / builders for ShortenedUrl, ClickEvent
- [ ] Coverage report via JaCoCo (target: 80%+ line coverage)

---

## Outputs
- Test files under `src/test/java/com/urlshortener/`
- JaCoCo coverage report under `reports/coverage/`
- Test results summary in `reports/test-results.md`
