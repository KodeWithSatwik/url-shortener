# Non-Functional Requirements — URL Shortener Service

**Version**: 1.0
**Phase**: Greenfield (Phase 1)

---

## NFR-001: Performance
- Redirect (GET /{shortCode}) latency: p99 < 50ms under normal load.
- URL shortening (POST) latency: p99 < 200ms.
- System is read-heavy: redirect:write ratio expected ~100:1.

## NFR-002: Availability
- Target availability: 99.9% (allows ~8.7 hours downtime/year).
- Graceful degradation: if analytics DB write fails, redirect MUST still succeed.

## NFR-003: Scalability
- Prototype: single JVM, H2 in-memory (sufficient for demonstration).
- Design MUST be stateless at the controller/service layer so horizontal scaling is possible with a shared persistent DB (PostgreSQL) and shared cache (Redis) in production.

## NFR-004: Security
- Reject any URL that does not start with `http://` or `https://`.
- Rate limit: max 20 requests/minute per IP (configurable).
- No open redirect: only stored URLs can be redirected to (no arbitrary URL injection via query param).
- No sensitive data logged (no full URLs in INFO logs, only short codes).

## NFR-005: Maintainability
- Code coverage: minimum 80% line coverage (enforced via JaCoCo).
- All public APIs documented via OpenAPI 3.0.
- Architecture Decision Records (ADRs) maintained for all significant decisions.
- Database schema managed exclusively via Flyway migrations (no Hibernate DDL auto-create).

## NFR-006: Observability
- Spring Actuator health and metrics endpoints exposed.
- All workflow/orchestration decisions written to an audit log.
- Structured log format (Spring default with timestamps and levels).

## NFR-007: Reliability
- Short code generation MUST retry up to 5 times on collision before failing.
- Analytics recording failures MUST be logged but MUST NOT propagate to the caller.
- Flyway migration failures MUST prevent application startup (fail-fast).
