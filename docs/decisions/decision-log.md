# Decision Log — URL Shortener Service

All significant engineering decisions made during the project.

---

| # | Phase | Decision | Choice | Rationale Summary | ADR |
|---|-------|----------|--------|-------------------|-----|
| 1 | Phase 0 | Language & Framework | Java 17 + Spring Boot 3 | LTS, Jakarta EE 10, mature ecosystem; Java 21 unavailable on build machine (downgraded at Phase 0) | — |
| 2 | Phase 1 | Short code generation | Base62 of auto-increment ID | Guaranteed uniqueness, no collision, simple | ADR-001 |
| 3 | Phase 1 | Database | H2 (dev) + PostgreSQL (prod) | Zero-setup dev, production-grade prod | ADR-002 |
| 4 | Phase 1 | Analytics storage | Same DB, separate table | Simplicity, async writes don't block redirects | ADR-003 |
| 5 | Phase 1 | Redirect status code | HTTP 302 | Analytics accuracy — 301 would be cached by browsers | ADR-004 |
| 6 | Phase 1 | Rate limiting | Bucket4j in-memory token bucket | Standard Java library, no external dependency | ADR-005 |
| 7 | Phase 1 | Schema migrations | Flyway | Version-controlled, fail-fast on mismatch | — |
| 8 | Phase 1 | API documentation | SpringDoc OpenAPI | Auto-generated from annotations, Swagger UI | — |
| 9 | Phase 1 | Async analytics | Spring @Async with thread pool | Redirect latency must not be affected by analytics DB write | — |
| 10 | Phase 5 | Custom alias scope | Optional field on existing endpoint | Minimal surface change, backward compatible | — |
| 11 | Phase 6 | User management scope | API key auth, URL ownership, no OAuth | Minimal, defensible, backward compatible with anonymous URLs | — |
| 12 | Phase 7 | Orchestration model | In-process stateful engine, REST API | Demonstrates all required capabilities without distributed infra | — |

---

## Assumption Log

| # | Assumption | Impact if Wrong |
|---|------------|----------------|
| A-001 | Single tenant (no multi-tenancy in Phase 1–2) | Schema redesign, URL isolation needed |
| A-002 | No authentication in Phase 1 | Any caller can shorten or delete any URL |
| A-003 | Default TTL is 30 days | URLs expire sooner or later than expected |
| A-004 | Base URL is `http://localhost:8080` in dev | Short URLs won't resolve on other hosts |
| A-005 | Prototype runs on a single JVM instance | Rate limiting and orchestration state not shared |
| A-006 | No production deployment required | Prototype is runnable locally only |
