# Functional Requirements — URL Shortener Service

**Version**: 1.0
**Phase**: Greenfield (Phase 1)
**Status**: APPROVED

---

## FR-001: URL Shortening
- The system SHALL accept a valid HTTP/HTTPS URL and return a shortened URL.
- The system SHALL generate a unique short code of 6 characters (Base62: a–z, A–Z, 0–9).
- The system SHALL return the full shortened URL including the configured base URL (e.g., `http://localhost:8080/abc123`).
- The system SHALL detect duplicate long URLs and return the existing short code (idempotent by default).

## FR-002: URL Redirection
- The system SHALL redirect `GET /{shortCode}` to the original URL using HTTP 302 Found.
- The system SHALL return HTTP 404 if the short code does not exist.
- The system SHALL return HTTP 410 Gone if the URL has expired.

## FR-003: URL Deletion
- The system SHALL allow deletion of a shortened URL via `DELETE /api/v1/urls/{shortCode}`.
- The system SHALL return HTTP 204 No Content on successful deletion.
- The system SHALL return HTTP 404 if the short code does not exist.

## FR-004: Analytics
- The system SHALL record each redirect event (timestamp, referrer header, IP address).
- The system SHALL expose `GET /api/v1/urls/{shortCode}/analytics` returning:
  - Total click count
  - List of recent 20 click events (timestamp, referrer)
  - Top 5 referrers by count
- Analytics recording SHALL be asynchronous and MUST NOT block the redirect response.

## FR-005: URL Expiry
- The system SHALL support an optional `expiresAt` field (ISO-8601 datetime) on URL creation.
- The system SHALL default to 30 days TTL when no expiry is provided (configurable).
- Expired URLs SHALL return HTTP 410 Gone on redirect.

## FR-006: Input Validation
- The system SHALL reject non-HTTP/HTTPS URLs (e.g., `javascript:`, `ftp://`) with HTTP 400.
- The system SHALL reject URLs longer than 2048 characters with HTTP 400.
- The system SHALL reject malformed requests (missing URL, invalid JSON) with HTTP 400.

## FR-007: Health Check
- The system SHALL expose `GET /actuator/health` returning service health status.

---

## User Stories

| ID | As a... | I want to... | So that... |
|----|---------|--------------|------------|
| US-001 | API consumer | POST a long URL and receive a short URL | I can share compact links |
| US-002 | End user | Click a short URL and be redirected | I reach the destination seamlessly |
| US-003 | API consumer | Delete a short URL I created | I can remove links I no longer need |
| US-004 | API consumer | See how many times my link was clicked | I can measure engagement |
| US-005 | API consumer | Set an expiry date on a link | The link stops working after a deadline |

---

## Edge Cases

| Case | Expected Behaviour |
|------|-------------------|
| Same long URL submitted twice | Return existing short code (idempotent) |
| Short code collision (extremely rare) | Retry up to 5 times with new code |
| Expired URL redirect | HTTP 410 Gone |
| Short code not found | HTTP 404 Not Found |
| Deleted URL redirect | HTTP 404 Not Found |
| URL = `javascript:alert(1)` | HTTP 400 Bad Request |
| URL > 2048 chars | HTTP 400 Bad Request |
| Analytics for unknown code | HTTP 404 Not Found |
