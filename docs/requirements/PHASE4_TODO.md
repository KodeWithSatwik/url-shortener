# Phase 4 — Documentation & API Specs

**Status**: PENDING
**Gate Entry**: Tests pass (Phase 3 gate cleared).
**Gate Exit**: Human approves documentation completeness.

---

## Sub-Tasks

### 4.1 API Specification
- [ ] Verify SpringDoc auto-generated OpenAPI spec at `/v3/api-docs`
- [ ] Add `@Operation`, `@ApiResponse`, `@Schema` annotations to all endpoints
- [ ] Export `openapi.yaml` to `docs/api-specs/openapi.yaml`
- [ ] Verify Swagger UI works at `/swagger-ui.html`

### 4.2 README
- [ ] Project overview and purpose
- [ ] Prerequisites (Java 21, Maven 3.9+)
- [ ] How to build: `./mvnw clean package`
- [ ] How to run: `./mvnw spring-boot:run`
- [ ] How to run tests: `./mvnw test`
- [ ] Environment variables / config reference
- [ ] API quick-start examples (curl commands)
- [ ] Architecture summary

### 4.3 Architecture Overview
- [ ] `docs/architecture/architecture-overview.md` — component diagram (ASCII or Mermaid)
- [ ] Data flow for shorten operation
- [ ] Data flow for redirect operation
- [ ] Orchestration layer overview
- [ ] Technology stack table with rationale

### 4.4 Decision Log
- [ ] `docs/decisions/decision-log.md` — all key decisions made during the project
  - Stack selection
  - Short code strategy
  - Redirect status code choice
  - Analytics async vs sync
  - Rate limiting approach
  - Orchestration design choices

---

## Outputs
- `README.md` at project root
- `docs/api-specs/openapi.yaml`
- `docs/architecture/architecture-overview.md`
- `docs/decisions/decision-log.md`
