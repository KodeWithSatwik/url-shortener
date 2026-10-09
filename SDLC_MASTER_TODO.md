# URL Shortener — Agentic SDLC Master Todo

> Assignment: Agentic Software Engineering System — URL Shortener
> Stack: Java 17 + Spring Boot 3 + Jakarta EE (Jakarta Persistence, Validation, Servlet)
> Orchestration: Agentic process model with dependency graph, gates, retries, audit trail
> Human Role: Oversight, approvals at gates, final quality control

---

## Phase 0 — Project Setup & Workspace Initialization
- [x] Read and parse assignment PDF
- [x] Create workspace directory structure
- [x] Write SDLC master todo and per-phase todos
- [x] Scaffold Spring Boot 3 project (pom.xml, application.yml, main class)
- [x] Set up database (H2 for dev, schema migration via Flyway)
- [x] Set up base build pipeline (Maven wrapper)

**Gate 0 EXIT**: ✅ Human approved project structure and stack choices — "yes kick off"

---

## Phase 1 — Requirements & Architecture (Greenfield Scenario)
See: `docs/requirements/PHASE1_TODO.md`

- [x] Write formal requirements document (functional + non-functional)
- [x] Identify and resolve ambiguities (document decisions)
- [x] Design system architecture (components, data flows, API surface)
- [x] Write Architecture Decision Records (ADRs 001–005)
- [x] Define data models (URL entity, analytics)
- [x] Define API contracts (OpenAPI 3.0 via SpringDoc annotations)

**Gate 1 EXIT**: ✅ Human approved architecture and API contracts — proceeded to implementation

---

## Phase 2 — Implementation (Core URL Shortener Service)
See: `docs/requirements/PHASE2_TODO.md`

- [x] Implement URL entity and repository (Spring Data JPA / Jakarta Persistence)
- [x] Implement URL shortening logic (Base62 encoding, collision handling via DB sequence)
- [x] Implement POST /api/v1/urls shorten endpoint
- [x] Implement GET /{shortCode} redirect endpoint (HTTP 302)
- [x] Implement DELETE /api/v1/urls/{shortCode} endpoint
- [x] Implement analytics (click count, referrer, timestamp — async)
- [x] Implement GET /api/v1/urls/{shortCode}/analytics endpoint
- [x] Implement expiry/TTL support (410 Gone on expired links)
- [x] Add input validation (Jakarta Validation — URL format, scheme, length)
- [x] Add global exception handling (GlobalExceptionHandler)
- [x] Add rate limiting (Bucket4j per-IP token bucket)

**Gate 2 EXIT**: ✅ Human reviewed implementation — tests requested

---

## Phase 3 — Testing
See: `docs/requirements/PHASE3_TODO.md`

- [x] Unit tests — URL shortening logic (Base62, collision)
- [x] Unit tests — Service layer (mock repository)
- [x] Integration tests — Full API via MockMvc (14 scenarios)
- [x] Integration tests — Analytics tracking
- [x] Edge case tests — Expired URLs, invalid codes, duplicate long URLs, custom alias conflict
- [x] Test coverage report (JaCoCo — see `reports/coverage/`)

**Gate 3 EXIT**: ✅ 36/36 tests pass, 0 failures — human approved

---

## Phase 4 — Documentation & API Specs
See: `docs/requirements/PHASE4_TODO.md`

- [x] OpenAPI 3.0 spec (auto-generated via SpringDoc; exported to `docs/api-specs/openapi.yaml`)
- [x] README with setup instructions
- [x] Architecture overview document
- [x] Orchestration design document

**Gate 4 EXIT**: ✅ Documentation complete

---

## Phase 5 — Brownfield Scenario (Enhancement/Refactor)
See: `docs/scenarios/PHASE5_TODO.md`

- [x] Scenario definition: add custom alias support to existing URL shortener
- [x] Impact analysis (which modules/services/data flows are affected)
- [x] Implement custom alias feature (minimal diff: 4 files changed)
- [x] Update tests for custom alias (2 new tests; all 36 green, no regressions)
- [x] Update API spec and docs

**Gate 5 EXIT**: ✅ Brownfield change complete, no regressions

---

## Phase 6 — Ambiguous Requirements Scenario
See: `docs/scenarios/PHASE6_TODO.md`

- [x] Receive ambiguous requirement: "add user management"
- [x] Agent interprets, identifies 7 distinct ambiguities
- [x] Normalize into a clear engineering problem with scoped interpretation
- [x] Document interpretation decisions and explicit out-of-scope boundaries
- [ ] Implement scoped solution — **BLOCKED: awaiting Gate 6 human approval**

**Gate 6 EXIT**: ⏳ PENDING — Agent's interpretation document ready at `docs/scenarios/ambiguous-requirements.md`. Human must approve scope before implementation proceeds.

---

## Phase 7 — Orchestration Design & Audit Trail
See: `docs/requirements/PHASE7_TODO.md`

- [x] Design dependency graph model (8 phases, explicit entry/exit gates)
- [x] Document orchestration model (process-level, not runtime Spring Bean)
- [x] Define entry/exit gates with human approval checkpoints
- [x] Define parallel + sequential path structure (phases 4/5/6 parallel)
- [x] Document cross-stage context store (todo list + phase files + ADRs + decision log)
- [x] Document bounded retry + fault tolerance + rollback procedure
- [x] Implement policy guardrails (security: URL scheme validation, rate limiting; compliance: Flyway-only migrations; change control: brownfield impact analysis)
- [x] Write audit log (every decision, transition, approval — `docs/decisions/audit-log.md`)
- [x] Document reliability metrics (success rate, MTTR, latency — `orchestration-design.md` §8)
- [x] Document dynamic re-planning evidence (`orchestration-design.md` §9)

**Gate 7 EXIT**: ✅ Orchestration design documented — human confirmed model

---

## Phase 8 — Release Readiness & Final Engineering Summary
See: `docs/requirements/PHASE8_TODO.md`

- [x] Final end-to-end run (all APIs, all scenarios)
- [x] Write Final Engineering Summary (plan, artifacts, risks, trade-offs, assumptions, limitations)
- [x] Write setup instructions (README)
- [x] Review all three scenarios — each shows decomposition, orchestration, validation
- [x] Export OpenAPI spec to `docs/api-specs/openapi.yaml`
- [x] Confirm all deliverables are present
- [ ] Gate 8: **PENDING** — Final human sign-off (Phase 6 implementation still awaiting Gate 6)

**Gate 8 EXIT**: ⏳ PENDING — All deliverables except Phase 6 implementation are complete and verified.

---

## Workspace Structure

```
url-shortener/
├── SDLC_MASTER_TODO.md         ← this file
├── README.md                   ← setup + quickstart
├── docs/
│   ├── FINAL_ENGINEERING_SUMMARY.md   ← plan, artifacts, risks, assumptions, limitations
│   ├── requirements/            ← per-phase todos + requirement docs
│   ├── architecture/            ← architecture-overview.md, orchestration-design.md, ADRs
│   ├── scenarios/               ← greenfield, brownfield, ambiguous
│   ├── api-specs/               ← openapi.yaml (exported)
│   └── decisions/               ← decision-log.md, audit-log.md
├── src/
│   ├── main/java/com/urlshortener/
│   │   ├── controller/          ← UrlController, AnalyticsController
│   │   ├── service/             ← UrlShortenerService, AnalyticsService, Base62Encoder
│   │   ├── repository/          ← ShortenedUrlRepository, ClickEventRepository
│   │   ├── model/               ← ShortenedUrl, ClickEvent (JPA entities)
│   │   ├── config/              ← AppConfig, AsyncConfig, RateLimitFilter
│   │   ├── dto/                 ← ShortenUrlRequest/Response, AnalyticsResponse, ErrorResponse
│   │   └── exception/           ← domain exceptions + GlobalExceptionHandler
│   └── test/
│       ├── unit/                ← Base62EncoderTest, UrlShortenerServiceTest (22 tests)
│       └── integration/         ← UrlShortenerIntegrationTest (14 tests)
├── scripts/
│   └── load-test.sh             ← 100-request load test across 10 simulated IPs
└── reports/
    └── coverage/                ← JaCoCo HTML + XML + CSV reports
```
