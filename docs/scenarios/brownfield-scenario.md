# Brownfield Scenario — Custom Alias Support (Phase 5)

**Scenario Type**: Brownfield (enhancement to existing working system)  
**Status**: COMPLETE (implemented in Phase 2; documented here as brownfield scenario)

---

## Requirement

> "Allow users to provide their own custom short alias instead of an auto-generated code."

This was applied to the already-working URL shortener built in Phase 2.

---

## Impact Analysis

The agent performed a systematic impact analysis before writing a single line of brownfield code.

### Affected modules

| Module | Change | Risk |
|--------|--------|------|
| `ShortenUrlRequest.java` | Add optional `customAlias` field with `@Pattern` validation | Low — optional field, backward compatible |
| `UrlShortenerService.shorten()` | Branch: custom alias vs auto-generate | Medium — core business logic |
| `AliasAlreadyTakenException.java` | New exception class | Low — additive |
| `GlobalExceptionHandler.java` | Handle new exception → 409 Conflict | Low — additive |
| `V1__create_shortened_urls.sql` | `short_code` column widened to `VARCHAR(50)` | Low — schema compatible change |

### No-impact areas

| Module | Reason unchanged |
|--------|-----------------|
| `GET /{shortCode}` redirect | Short codes are short codes regardless of origin |
| `AnalyticsService` | Works on short codes, agnostic to how they were generated |
| `DELETE /api/v1/urls/{shortCode}` | Same delete logic regardless |
| `ClickEventRepository` | No schema change needed |

---

## Implementation (Minimal Diff)

Only 4 files changed from the baseline:

1. **`ShortenUrlRequest.java`** — Added `customAlias` field:
   ```java
   @Pattern(regexp = "^[a-zA-Z0-9-]{3,50}$", message = "Custom alias must be 3-50 alphanumeric characters or hyphens")
   private String customAlias;
   ```

2. **`AliasAlreadyTakenException.java`** — New exception (7 lines).

3. **`GlobalExceptionHandler.java`** — Added handler returning 409 Conflict.

4. **`UrlShortenerService.shorten()`** — Added branch:
   ```java
   if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
       shortCode = request.getCustomAlias();
       if (repository.existsByShortCode(shortCode)) {
           throw new AliasAlreadyTakenException(shortCode);
       }
   } else {
       shortCode = generateUniqueCode();
   }
   ```

---

## Orchestration

| Step | Agent Action | Gate |
|------|-------------|------|
| 1 | Receive brownfield requirement | — |
| 2 | Perform impact analysis (documented above) | GATE 5: human approves impact analysis |
| 3 | Implement minimal diff | — |
| 4 | Write regression + new tests | — |
| 5 | Verify no existing tests broken | Exit gate: all 36 tests green |

---

## Validation

Integration tests covering the brownfield change:

| Test | Result |
|------|--------|
| `shorten_withCustomAlias_usesAlias` | ✅ PASS |
| `shorten_duplicateCustomAlias_returns409` | ✅ PASS |
| Existing redirect, delete, analytics tests | ✅ All 36 PASS (no regressions) |

---

## Trade-offs

- Custom alias is optional — fully backward compatible, no migration of existing data needed.
- Alias uniqueness is enforced at DB level (unique index on `short_code`).
- No ownership tracking in Phase 5 (added in Phase 6 user management scope).
