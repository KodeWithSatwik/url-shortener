# Greenfield Scenario — URL Shortener Service (Phase 1–3)

**Scenario Type**: Greenfield (new system from scratch)  
**Status**: COMPLETE

---

## What Was Built

A production-quality URL shortener REST API using Java 17 + Spring Boot 3, built from scratch with zero existing code.

## Decomposition

The agent decomposed the greenfield requirement into:

1. **Requirement Understanding** — Functional + non-functional requirements documented, edge cases identified, ambiguities resolved (multi-tenancy, auth, custom alias all scoped out with documented assumptions).
2. **Architecture** — Component diagram, two data flow diagrams (shorten + redirect), 5 ADRs covering every significant design decision.
3. **Implementation** — 15 source files: entities, repositories, services, controllers, DTOs, exception handlers, config.
4. **Testing** — 36 tests: 22 unit, 14 integration. All green.

## Orchestration

| Step | Agent Action | Gate |
|------|-------------|------|
| 1 | Parse requirements, identify scope | — |
| 2 | Write architecture docs & ADRs | GATE 1: human approval |
| 3 | Implement all code | GATE 2: human review |
| 4 | Write & run all tests | GATE 3: test results |

## Validation

- All 36 tests pass (unit + integration)
- Input validation: `javascript:`, `ftp://`, empty URL, URL >2048 chars all return 400
- Idempotency: same URL submitted twice returns existing short code
- Expiry: expired URLs return 410 Gone
- Delete: deleted URLs return 404 on redirect
- Analytics: click events recorded asynchronously, do not block redirect

## Key Decisions (from ADRs)

| Decision | Choice | Why |
|----------|--------|-----|
| Short code generation | Base62 of DB sequence | Guaranteed uniqueness, no collision, simple |
| Redirect status | HTTP 302 | Analytics accuracy — 301 would be browser-cached |
| Analytics | Async `@Async` write | Redirect latency must not be impacted |
| Rate limiting | Bucket4j token bucket per-IP | No external dependency, burst-tolerant |
