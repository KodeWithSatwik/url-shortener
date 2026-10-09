# Phase 5 — Brownfield Scenario: Custom Alias Support

**Status**: PENDING
**Gate Entry**: Human approved Phase 4 docs. Brownfield scenario begins on an already-working system.
**Gate Exit**: Human approves brownfield change, impact analysis, and updated tests/docs.

---

## Scenario Definition
**Requirement**: "Allow users to provide their own custom short alias instead of an auto-generated code."
**Type**: Brownfield — enhancement to existing URL shortener service.

---

## Sub-Tasks

### 5.1 Impact Analysis (Brownfield Reasoning)
- [ ] Identify affected modules:
  - `ShortenUrlRequest` — add optional `customAlias` field
  - `UrlShortenerService.shorten()` — branch: custom alias vs auto-generate
  - `ShortenedUrlRepository` — uniqueness constraint already exists on shortCode (no schema change needed)
  - `GlobalExceptionHandler` — new error: alias already taken
  - Validation: alias format (alphanumeric + hyphen, 3-50 chars)
- [ ] Identify NO-IMPACT areas: redirect logic, analytics, delete — unchanged
- [ ] Document impact analysis in `docs/scenarios/brownfield-impact-analysis.md`

### 5.2 Implementation
- [ ] Update `ShortenUrlRequest.java` — add `@Pattern`-validated `customAlias` field (optional)
- [ ] Add `AliasAlreadyTakenException.java`
- [ ] Update `UrlShortenerService.shorten()` — if customAlias provided: validate uniqueness, use it; else auto-generate
- [ ] Update `GlobalExceptionHandler` — handle `AliasAlreadyTakenException` → 409 Conflict
- [ ] Add `V3__add_alias_index.sql` Flyway migration (index on shortCode already exists, verify)

### 5.3 Updated Tests
- [ ] `CustomAliasTest.java` — shorten with valid custom alias
- [ ] `CustomAliasTest.java` — alias already taken → 409
- [ ] `CustomAliasTest.java` — alias with invalid characters → 400
- [ ] `CustomAliasTest.java` — alias too short / too long → 400
- [ ] Regression: existing auto-generate flow still works

### 5.4 Documentation Updates
- [ ] Update `openapi.yaml` — document `customAlias` field
- [ ] Update `README.md` — add custom alias example
- [ ] Update decision log — document brownfield change rationale

---

## Outputs
- Updated source files (minimal diff — only what changed)
- `docs/scenarios/brownfield-impact-analysis.md`
- `docs/scenarios/brownfield-scenario.md` — decomposition + orchestration + validation narrative
