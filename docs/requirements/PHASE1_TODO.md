# Phase 1 — Requirements & Architecture (Greenfield Scenario)

**Status**: PENDING
**Gate Entry**: Phase 0 scaffold complete, human approved project structure.
**Gate Exit**: Human approves architecture + API contracts before any code is written.

---

## Sub-Tasks

### 1.1 Functional Requirements
- [ ] List all functional requirements (shorten, redirect, delete, analytics, expiry)
- [ ] Define user stories (as a user, I want to...)
- [ ] Identify edge cases (duplicate URLs, expired links, invalid codes, collision)

### 1.2 Non-Functional Requirements
- [ ] Define throughput targets (read-heavy: redirects >> writes)
- [ ] Define latency SLA (redirect < 50ms p99)
- [ ] Define availability target (99.9%)
- [ ] Define security requirements (input validation, rate limiting, no open redirect)
- [ ] Define observability requirements (metrics, audit log)

### 1.3 Ambiguity Identification
- [ ] Multi-tenancy? (assume single-tenant for now, document assumption)
- [ ] Authentication required? (assume none for Phase 1, added in Phase 6)
- [ ] Custom aliases? (Phase 1: auto-generate only; Phase 5 brownfield adds custom alias)
- [ ] URL expiry? (include, document default TTL)
- [ ] Analytics depth? (click count + timestamp + referrer for Phase 1)

### 1.4 Architecture Design
- [ ] Component diagram (Controller → Service → Repository → DB)
- [ ] Data flow diagram (POST /shorten → generate code → persist → return short URL)
- [ ] Data flow diagram (GET /{code} → lookup → increment analytics → redirect)
- [ ] Technology choices with rationale (Spring Boot 3, H2/PostgreSQL, Flyway, SpringDoc)

### 1.5 Architecture Decision Records (ADRs)
- [ ] ADR-001: Short code generation strategy (Base62 vs UUID vs Hash)
- [ ] ADR-002: Database choice (H2 dev / PostgreSQL prod)
- [ ] ADR-003: Analytics storage (same DB vs separate)
- [ ] ADR-004: Redirect HTTP status code (301 vs 302)
- [ ] ADR-005: Rate limiting strategy

### 1.6 Data Model
- [ ] ShortenedUrl entity (id, shortCode, originalUrl, createdAt, expiresAt, clickCount)
- [ ] ClickEvent entity (id, shortCode, timestamp, referrer, ipAddress)
- [ ] Database schema (Flyway migration V1)

### 1.7 API Contract (OpenAPI 3.0)
- [ ] POST /api/v1/urls — shorten a URL
- [ ] GET /{shortCode} — redirect to original URL
- [ ] DELETE /api/v1/urls/{shortCode} — delete a short URL
- [ ] GET /api/v1/urls/{shortCode}/analytics — get click analytics
- [ ] GET /api/v1/health — health check

---

## Outputs
- `docs/requirements/functional-requirements.md`
- `docs/requirements/non-functional-requirements.md`
- `docs/architecture/architecture-overview.md`
- `docs/architecture/adr/ADR-001.md` through `ADR-005.md`
- `docs/api-specs/openapi.yaml`
- `docs/decisions/decision-log.md`
