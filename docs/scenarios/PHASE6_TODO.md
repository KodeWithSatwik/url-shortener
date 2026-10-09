# Phase 6 — Ambiguous Requirements Scenario: "Add User Management"

**Status**: PENDING
**Gate Entry**: Human approved Phase 5 brownfield scenario.
**Gate Exit**: Human approves the agent's interpretation and scoped solution before implementation.

---

## Scenario Definition
**Raw Requirement** (intentionally ambiguous): "Add user management to the URL shortener."

**What's ambiguous**:
- Does "user management" mean registration/login? Admin-only management? OAuth? API keys?
- Should existing URLs be migrated to a user? Or only new URLs are owned?
- What happens to anonymous URLs (created before this feature)?
- What authorization model? (owner-only delete? admin can see all?)
- What authentication mechanism? (Basic Auth, JWT, session, API key?)

---

## Sub-Tasks

### 6.1 Agent Interpretation & Clarification
- [ ] Document all identified ambiguities
- [ ] Make scoped, defensible interpretation decisions (document each)
- [ ] Normalize into a clear engineering problem statement
- [ ] Present interpretation to human for approval (Gate 6 entry checkpoint)

**Agent's proposed interpretation**:
> Scope: Add per-user URL ownership using API key authentication.
> Users register via POST /api/v1/users with email+password, receive an API key.
> URLs created with an API key are owned by that user.
> Only the owner (or admin) can delete their URLs.
> Anonymous URL creation still allowed (backward compatible).
> No OAuth, no session management — keep it minimal.

### 6.2 Implementation (post human approval)
- [ ] `User.java` — JPA entity (id, email, passwordHash, apiKey, role, createdAt)
- [ ] `V4__create_users.sql` — Flyway migration
- [ ] `V5__add_owner_to_shortened_urls.sql` — add nullable `userId` FK to ShortenedUrl
- [ ] `UserRepository.java` — findByApiKey, findByEmail
- [ ] `UserService.java` — register, generateApiKey
- [ ] `UserController.java` — POST /api/v1/users (register), GET /api/v1/users/me
- [ ] `ApiKeyAuthFilter.java` — Jakarta Servlet filter extracting X-API-Key header
- [ ] Update `UrlShortenerService.shorten()` — associate URL with user if API key present
- [ ] Update `UrlShortenerService.delete()` — check ownership before delete
- [ ] `AuthorizationService.java` — canDelete(shortCode, apiKey)

### 6.3 Tests
- [ ] `UserRegistrationTest.java` — register, duplicate email → 409
- [ ] `ApiKeyAuthTest.java` — create URL with API key, verify ownership
- [ ] `OwnershipAuthorizationTest.java` — owner can delete, non-owner gets 403
- [ ] `AnonymousBackwardCompatTest.java` — anonymous URL creation still works

### 6.4 Documentation
- [ ] `docs/scenarios/ambiguous-requirements.md` — full narrative:
  - Raw requirement
  - Ambiguities identified
  - Interpretation decisions with rationale
  - Scope boundaries (what was NOT built and why)
  - Impact on existing system
- [ ] Update `openapi.yaml` — user endpoints, API key header
- [ ] Update `README.md` — authentication section

---

## Outputs
- Minimal new source files for user management
- `docs/scenarios/ambiguous-requirements.md`
- Updated tests
