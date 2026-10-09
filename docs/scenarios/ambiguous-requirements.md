# Ambiguous Requirements Scenario — "Add User Management" (Phase 6)

**Scenario Type**: Ambiguous requirement  
**Status**: SCOPED & DOCUMENTED (implementation deferred pending human approval — see Gate 6)

---

## Raw Requirement

> "Add user management to the URL shortener."

This is intentionally ambiguous. The agent's job is to identify ambiguities, make defensible scoping decisions, and present them for human approval before writing any code.

---

## Ambiguities Identified

| # | Ambiguity | Why It Matters |
|---|-----------|---------------|
| 1 | What does "user management" mean? Registration? Admin panel? OAuth? API keys? | Determines entire implementation scope |
| 2 | Does every URL need an owner, or is it optional? | Affects schema (nullable vs required FK) and backward compatibility |
| 3 | What happens to URLs created before this feature? | Data migration or orphaned records? |
| 4 | Authentication mechanism? Basic Auth, JWT, sessions, API keys? | Determines security model and client complexity |
| 5 | Authorization model? Owner-only delete? Admin sees all? | Affects every existing write endpoint |
| 6 | Is email verification required? | Scope creep risk — ops complexity |
| 7 | Multi-tenancy? Can users see each other's URLs? | Affects all queries and data isolation |

---

## Agent's Interpretation (Proposed Scope)

> **⚠️ This requires human approval (Gate 6) before implementation proceeds.**

The agent proposes the following scoped interpretation:

**Scope**: Per-user URL ownership via API key authentication.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Auth mechanism | API key (header: `X-API-Key`) | Simplest, stateless, no session management needed, adequate for a prototype |
| Registration | `POST /api/v1/users` with email + password | Minimal — no email verification, no OAuth |
| URL ownership | Optional (nullable `userId` FK) | Backward compatible — anonymous URL creation still works |
| Authorization | Owner can delete their own URLs; anonymous URLs are unprotected | Minimal change to existing endpoints |
| Admin role | Out of scope | Not mentioned in the requirement, adds significant complexity |
| Multi-tenancy | Out of scope | Assumption A-001 from Phase 1 maintained |
| Password storage | BCrypt hash | Industry standard, Spring Security provides it |

---

## What Is Explicitly Out of Scope

- Email verification
- Password reset
- OAuth / SSO
- Admin dashboard
- Pagination of user's URL list
- URL visibility rules between users
- JWT tokens or session management

These are documented as **not in scope** rather than silently omitted, so the human can override any of them.

---

## Proposed Implementation Plan (Pending Approval)

If the human approves the scope above, the agent will:

1. Add `V4__create_users.sql` — `users` table (id, email, password_hash, api_key, created_at)
2. Add `V5__add_owner_to_shortened_urls.sql` — nullable `user_id` FK on `shortened_urls`
3. Add `User.java` entity, `UserRepository.java`
4. Add `UserService.java` — register, generate API key (UUID)
5. Add `UserController.java` — `POST /api/v1/users`, `GET /api/v1/users/me`
6. Add `ApiKeyAuthFilter.java` — Jakarta Servlet filter extracting `X-API-Key`
7. Update `UrlShortenerService.shorten()` — associate URL with user if API key present
8. Update `UrlShortenerService.delete()` — check ownership (owner or anonymous-created)
9. Add tests: registration, API key auth, ownership authorization, backward compatibility

**Estimated impact**: 8 new/modified files. No existing tests broken (backward compatible).

---

## Orchestration

| Step | Agent Action | Gate |
|------|-------------|------|
| 1 | Receive ambiguous requirement | — |
| 2 | Identify all ambiguities | — |
| 3 | Propose scoped interpretation | **GATE 6: Human must approve before any code is written** |
| 4 | Implement scoped solution | Post-approval only |
| 5 | Run regression + new tests | Exit gate: all tests green |

---

## Why This Demonstrates Correct Agent Behaviour

A naive agent would silently pick one interpretation and implement it. This agent:

1. **Stopped** before writing code
2. **Named every ambiguity** explicitly
3. **Proposed a defensible, minimal scope** with rationale for each decision
4. **Drew explicit out-of-scope boundaries** to prevent scope creep
5. **Waited for human approval** before proceeding

This is controlled autonomy — the agent executes within defined boundaries, the human owns the scope decision.
