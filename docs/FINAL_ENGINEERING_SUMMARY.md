# Final Engineering Summary — URL Shortener Agentic SDLC

**Assignment**: Build an Agentic Software Engineering System — URL Shortener  
**Agent**: (Claude-based LLM agent)  
**Human**: Pavan Satwik Godavarthi
**Completed**: 2026-10-09

---

## 1. Plan & Rationale

### Approach
The assignment was decomposed into 8 SDLC phases using an explicit dependency graph. Each phase had documented entry/exit gates with human approval checkpoints. The agent executed each phase autonomously within defined boundaries; the human provided oversight and final approvals.

### Stack Choice
**Java 17 + Spring Boot 3 + Jakarta EE 10** was chosen because:
- Spring Boot 3 is the current LTS baseline for production Java web services
- Jakarta EE 10 (not javax) is the correct modern namespace
- Spring Data JPA + Flyway is the industry standard for managed schema evolution
- H2 eliminates all local setup friction while Flyway migrations are PostgreSQL-compatible
- The stack is unambiguously familiar to Java engineers reviewing this work

### Orchestration Model
Rather than building a workflow engine inside the application (which would be meta-engineering, not engineering), the orchestration is the *process itself*: the agent as executor, the human as approver, the todo list as the dependency graph, and the docs as the audit trail. This is a more honest demonstration of agentic SDLC than wrapping Spring Boot beans in a state machine.

---

## 2. Artifacts

| Artifact | Location | Description |
|----------|----------|-------------|
| Source code | `src/main/java/com/urlshortener/` | 15 production Java files |
| Tests | `src/test/java/com/urlshortener/` | 36 tests (unit + integration) |
| DB migrations | `src/main/resources/db/migration/` | V1–V3 Flyway migrations |
| Functional requirements | `docs/requirements/functional-requirements.md` | All FRs with user stories and edge cases |
| Non-functional requirements | `docs/requirements/non-functional-requirements.md` | Performance, security, availability targets |
| Architecture overview | `docs/architecture/architecture-overview.md` | Component diagram, data flows, tech stack |
| ADR-001 to ADR-005 | `docs/architecture/adr/` | Every significant design decision |
| Orchestration design | `docs/architecture/orchestration-design.md` | Dependency graph, gates, fault tolerance, rollback, metrics |
| Decision log | `docs/decisions/decision-log.md` | Consolidated table of all decisions |
| Audit log | `docs/decisions/audit-log.md` | Timestamped stage transitions and approvals |
| OpenAPI spec | `docs/api-specs/openapi.yaml` | Full OpenAPI 3.0.3 spec — all endpoints, schemas, examples |
| Greenfield scenario | `docs/scenarios/greenfield-scenario.md` | Decomposition + orchestration + validation |
| Brownfield scenario | `docs/scenarios/brownfield-scenario.md` | Impact analysis + minimal diff + regression proof |
| Ambiguous requirements | `docs/scenarios/ambiguous-requirements.md` | Ambiguity identification + scoped interpretation (pending Gate 6) |
| Dockerfile | `Dockerfile` | Multi-stage: Maven+JDK 17 build layer → JRE-only runtime (~220 MB image) |
| Docker Compose | `docker-compose.yml` | App + PostgreSQL 16 with health checks, persistent volume, .env-based secrets |
| Kubernetes manifests | `k8s/` | Deployment (2 replicas, probes, limits), Service, ConfigMap, Secret template |
| GitHub Actions CI | `.github/workflows/ci.yml` | Build + test + coverage upload on every push/PR |
| README | `README.md` | Setup, Docker quickstart, k8s deploy, CI, API usage, architecture summary |
| SDLC master todo | `SDLC_MASTER_TODO.md` | Full phase tracker with gates and completion status |

---

## 3. Risks & Trade-offs

| Risk / Trade-off | Detail | Mitigation |
|-----------------|--------|-----------|
| H2 in-memory only | Not suitable for production; resets on restart | `docker compose up` runs the full stack against PostgreSQL 16 automatically |
| In-memory rate limiting | Resets on restart; not shared across JVM instances | Replace with Bucket4j + Redis for horizontal scale |
| Short codes are sequential | Base62 of auto-increment sequence means codes are guessable | Acceptable for URL shortener; switch to random NanoID if enumeration is a concern |
| No HTTPS | TLS not configured in app | Assumed reverse proxy (nginx/ALB) terminates TLS; `Cache-Control: no-store` on redirects prevents caching issues |
| No distributed locking | Concurrent `existsByShortCode` + `save` has a TOCTOU window | Acceptable for single-instance; use `SELECT FOR UPDATE` or optimistic locking at scale |
| Analytics eventual consistency | Async write means click count may lag by milliseconds | By design — redirect latency is the primary SLA |
| User management not implemented | Phase 6 scoped and documented, pending human approval | Interpretation doc ready; implementation is 8 files once approved |
| Unbounded table growth | Expired URLs accumulate over time without cleanup | `@Scheduled purgeExpiredAndDeleted()` runs nightly at 03:00; configurable via `app.cleanup.cron` |

---

## 4. Assumptions

| # | Assumption | Rationale |
|---|------------|-----------|
| A-001 | Single-tenant (no multi-tenancy) | Not mentioned in requirements; scoped out explicitly |
| A-002 | No authentication in Phase 1–3 | Greenfield scope; Phase 6 adds it |
| A-003 | Default URL TTL = 30 days | Reasonable default; fully configurable |
| A-004 | Base URL = `http://localhost:8080` in dev | Standard Spring Boot default |
| A-005 | Single JVM instance | Prototype scope; distributed concerns documented as limitations |
| A-006 | Full cloud deployment not required | Assignment asks for "runnable prototype"; Docker Compose provides a production-like stack locally |
| A-007 | Java 17 JDK available (not JRE) | Confirmed during Phase 0 |

---

## 5. Limitations

1. **Not horizontally scalable** without replacing in-memory rate limiter + DB sequence with Redis-backed alternatives.
2. **No email verification** for user registration (Phase 6 scope — accepted trade-off for prototype).
3. **No password reset** flow (same reason).
4. **Analytics at scale** — the `click_events` table will need time-series optimisation (TimescaleDB, ClickHouse) at billions of rows.
5. **Short code predictability** — sequential Base62 codes are enumerable. Production systems should use random NanoID.
6. **No HTTPS** — assumes reverse proxy in production.
7. **User management pending** — Phase 6 is scoped and documented but awaits human approval of the interpretation.

---

## 6. What This Demonstrates

| Assignment Criterion | How Met |
|---------------------|---------|
| Requirement understanding | Functional + non-functional requirements documented with explicit ambiguity resolution |
| Task decomposition | 8-phase dependency graph with 50+ sub-tasks and per-phase todo files |
| Codebase reasoning (brownfield) | Systematic impact analysis before any brownfield code written; documented in scenario doc |
| Workflow orchestration | Dependency graph, entry/exit gates, parallel paths (phases 4/5/6), human approval checkpoints, audit log, reliability metrics, re-planning evidence, rollback/safe-stop procedure |
| Engineering output quality | 15 production files, 36 tests, Flyway migrations, OpenAPI annotations, Jakarta Validation |
| Validation & risk control | Edge cases tested; risks documented with mitigations; policy guardrails in code |
| Controlled autonomy | Agent executed all code; human approved stack, architecture, brownfield scope, ambiguous scope |
| Three scenarios | Greenfield (phases 1–3) ✅ fully implemented; Brownfield (custom alias) ✅ fully implemented; Ambiguous (user management) ✅ scoped, interpretation documented, implementation **pending Gate 6 human approval** |
| Final engineering summary | This document |

---

## 7. Testing Strategy

### Test Structure

| Layer | File | Count | What Is Covered |
|-------|------|-------|-----------------|
| Unit — Base62 encoding | `Base62EncoderTest.java` | 10 | Encode/decode correctness, zero, max value, all-character alphabet coverage |
| Unit — Service logic | `UrlShortenerServiceTest.java` | 12 | Shorten, resolve, delete, expiry, deduplication, custom alias, collision retry |
| Integration — REST API | `UrlShortenerIntegrationTest.java` | 14 | End-to-end MockMvc tests covering all endpoints, status codes, and edge cases |
| **Total** | | **36** | **0 failures, 0 skipped** |

### JaCoCo Coverage (from `reports/coverage/jacoco.csv`)

| Package | Instruction Coverage | Branch Coverage |
|---------|---------------------|-----------------|
| `service` (UrlShortenerService, AnalyticsService, Base62Encoder) | **88%** | **73%** |
| `controller` (UrlController, AnalyticsController) | **87%** | **25%** (rate-limit path not hit in unit tests) |
| `config` (AppConfig, AsyncConfig, RateLimitFilter) | **79%** | **33%** |
| `exception` (GlobalExceptionHandler + 3 exceptions) | **81%** | n/a |
| `model` / `dto` | ~40% | ~5% (Lombok-generated builder methods not exercised) |

> Note: Low model/DTO coverage is expected — Lombok-generated `equals`, `hashCode`, `toString`, and builder methods are not under test, which is standard practice.

### What Is Explicitly Not Tested

| Gap | Reason | Mitigation |
|-----|--------|-----------|
| Contract/consumer-driven tests | Out of scope for prototype | OpenAPI spec at `docs/api-specs/openapi.yaml` serves as the contract |
| Security / penetration tests | Out of scope | URL scheme validation + rate limiting are tested at unit level |
| Performance / load tests | Not automated in CI | `scripts/load-test.sh` provides manual load verification (100 req, 10 IPs, 0.6s interval) |
| Distributed / multi-node scenarios | Single JVM prototype | Documented as limitation A-005 |
| Phase 6 (user management) tests | Implementation pending Gate 6 | Test plan documented in `ambiguous-requirements.md` §Proposed Implementation Plan |

### Load Test Script (`scripts/load-test.sh`)

Sends 100 requests across 4 operation types (POST shorten, GET redirect, GET analytics, GET health) distributed across 10 simulated client IPs via `X-Forwarded-For`. Each IP sends 10 requests — well under the 20 req/min rate limit — so no 429s are expected. Validates that all core paths return expected status codes under concurrent-ish load.

**To run:** start the app (`mvn spring-boot:run`), then `bash scripts/load-test.sh`.

---

## 8. How to Run Everything

```bash
cd url-shortener

# Build & test
mvn clean test

# Run
mvn spring-boot:run

# API docs
open http://localhost:8080/swagger-ui.html

# DB console
open http://localhost:8080/h2-console
```

All 36 tests pass. The service starts in ~3 seconds on the first run (Flyway migrations + H2 initialization).
