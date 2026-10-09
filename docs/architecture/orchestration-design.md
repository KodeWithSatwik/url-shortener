# Orchestration Design — Agentic SDLC Execution

**Version**: 1.0  
**Assignment**: Agentic Software Engineering System — URL Shortener  
**Agent**: AI Agent (LLM-based)
**Human (Oversight & Approvals)**: Pavan Satwik Godavarthi

---

## 1. Overview

This document describes the agentic orchestration model used to build the URL shortener service. The orchestration is the *process* — not a runtime library inside the application. The agent managed the full SDLC lifecycle as a stateful dependency graph, with the human providing oversight, approvals at gates, and final quality control.

**Principle**: Agents execute under defined autonomy boundaries. Humans own oversight, approvals, and final quality.

---

## 2. Dependency Graph

The SDLC was decomposed into eight stages with explicit dependencies and human approval gates:

```
[Phase 0: Setup]
      │
      ▼  ← GATE 0: Human approves project structure & stack
[Phase 1: Requirements & Architecture]
      │
      ▼  ← GATE 1: Human approves architecture + API contracts
[Phase 2: Implementation]
      │
      ▼  ← GATE 2: Human reviews code quality
[Phase 3: Testing]
      │
      ▼  ← GATE 3: Human reviews test results & coverage
[Phase 4: Documentation]          [Phase 5: Brownfield]   [Phase 6: Ambiguous Req]
      │                                   │                        │
      └───────────────────────────────────┴────────────────────────┘
                                          │
                                          ▼  ← GATE 7: Human reviews all three scenarios
                             [Phase 8: Release Readiness]
                                          │
                                          ▼  ← GATE 8: Human final sign-off
                                     [COMPLETE]
```

**Phases 4, 5, and 6 are parallel** — they have no mutual dependency and can be executed concurrently once Phase 3 is cleared.

---

## 3. Stage Definitions

| Stage | Entry Gate | Exit Gate | Human Approval Required | Parallel Paths |
|-------|-----------|-----------|------------------------|----------------|
| Phase 0: Setup | None | Stack choices documented | Yes — stack approval | No |
| Phase 1: Requirements | Phase 0 done | Architecture doc + ADRs + API spec written | Yes — architecture approval | No |
| Phase 2: Implementation | Phase 1 approved | All source files compile | Yes — code review | No |
| Phase 3: Testing | Phase 2 approved | All tests green, coverage recorded | Yes — test review | No |
| Phase 4: Docs | Phase 3 done | README, OpenAPI spec, architecture overview | Yes — doc completeness | Parallel with 5 & 6 |
| Phase 5: Brownfield | Phase 3 done | Impact analysis + implementation + tests | Yes — brownfield approval | Parallel with 4 & 6 |
| Phase 6: Ambiguous | Phase 3 done | Interpretation doc + scoped implementation | Yes — scope approval | Parallel with 4 & 5 |
| Phase 8: Release | 4 + 5 + 6 done | Full build passes, all deliverables present | Yes — final sign-off | No |

---

## 4. Human Approval Checkpoints

Each gate represents a point where the agent **stops and waits** for human direction before proceeding.

| Gate | What the Human Reviewed | Decision |
|------|------------------------|----------|
| Gate 0 | Java 17 + Spring Boot 3 stack, Maven project structure, workspace layout | **Approved** — "yes kick off" |
| Gate 1 | Architecture doc, 5 ADRs, functional/non-functional requirements | **Approved implicitly** — proceeded to implementation |
| Gate 2 | Full implementation: entities, services, controllers, config, exception handling | **Approved** — tests requested |
| Gate 3 | 36 tests, 0 failures | **Approved implicitly** |
| Gate 6 (Ambiguous) | Agent's interpretation of "add user management" | **Pending — see Phase 6 doc** |
| Gate 8 | Final deliverables checklist | **Pending — final sign-off** |

---

## 5. Cross-Stage Context & Decision Lineage

The agent maintained context across stages via:

1. **Todo list** — live dependency graph, visible to both agent and human at all times
2. **Phase todo files** (`docs/requirements/PHASE*.md`) — each stage's sub-tasks, entry/exit conditions, outputs
3. **ADRs** (`docs/architecture/adr/`) — every significant decision with rationale and alternatives rejected
4. **Decision log** (`docs/decisions/decision-log.md`) — consolidated table of all decisions
5. **Audit log** (`docs/decisions/audit-log.md`) — timestamped record of every stage transition and agent action

---

## 6. Fault Tolerance Model

| Scenario | Behaviour |
|----------|-----------|
| Compilation error | Agent reads error output, diagnoses root cause, applies targeted fix, re-compiles. No human needed unless root cause is ambiguous. |
| Test failure | Agent reads failure message, identifies exact failing assertion, fixes the source or test, re-runs. Human notified if fix is non-trivial. |
| Schema mismatch (Flyway) | Flyway fails fast at startup — agent diagnoses migration, adds corrective migration (`V3__...`), re-tests. |
| Ambiguous requirement | Agent stops, documents ambiguities, proposes scoped interpretation, **waits for human approval** before implementing (Gate 6). |
| Conflicting constraints | Agent documents the conflict as an ADR trade-off, chooses the safer option, records rationale. |
| Human rejects a decision | Human redirects, agent re-plans the affected stage and all downstream stages without discarding completed work. |

---

## 6a. Rollback & Safe-Stop Procedure

This section defines the formal rollback and safe-stop controls for each scenario where a completed stage must be reversed or execution halted.

### Stage Rollback Procedure

| Stage | Rollback Trigger | Rollback Action |
|-------|-----------------|-----------------|
| **Phase 0 (Setup)** | Human rejects stack/structure | Delete generated project files, re-scaffold from scratch with new choices. No data or schema exists yet. |
| **Phase 1 (Requirements/Architecture)** | Human rejects architecture | Discard `architecture-overview.md` and ADRs, rewrite from human-directed constraints. Phase 2 cannot start. |
| **Phase 2 (Implementation)** | Human rejects code quality | Revert specific files using git diff against Phase 1 baseline. Agent does not proceed to Phase 3 until human re-approves. |
| **Phase 3 (Testing)** | Test failures cannot be resolved | Roll back to last green commit in Phase 2, identify the failing source change, apply targeted fix, re-run. If fix is non-trivial: raise to human before continuing. |
| **Phase 5 (Brownfield)** | Impact analysis reveals higher risk than assessed | Revert the 4 changed files (`ShortenUrlRequest`, `AliasAlreadyTakenException`, `GlobalExceptionHandler`, `UrlShortenerService`) to their Phase 2 baseline. Schema column widening (`VARCHAR(50)`) is non-destructive and does not need reverting. |
| **Phase 6 (Ambiguous)** | Human rejects scope interpretation | Discard interpretation doc, produce a revised scoped interpretation, re-present at Gate 6. No code was written so no code rollback is needed. |
| **Phase 7 (Orchestration)** | Design rejected | Rewrite orchestration design doc; audit log entries are append-only and are not rolled back. |

### Safe-Stop Controls

A **safe-stop** halts forward progress without discarding completed work:

1. **Human veto at any gate** — The agent stops at the gate boundary, preserves all completed artifacts, and waits indefinitely for human direction. No stage that follows the gate is started.
2. **Unresolvable compilation error** — Agent stops after 3 retry attempts, produces a diagnostic report, and escalates to human before any further action.
3. **Test suite regression** — If a fix for one test breaks another, agent stops, documents the conflict, and escalates rather than entering a fix loop.
4. **Scope creep detected** — If an implementation task would affect more modules than the impact analysis predicted, agent stops, updates the impact analysis, and re-presents for human approval before continuing.

### Rollback vs. Re-plan

| Situation | Action |
|-----------|--------|
| Error in the current stage | Fix in place (retry) — no rollback needed |
| Error caused by a previous stage's output | Rollback the upstream stage, fix, re-execute downstream |
| Human rejects a decision | Re-plan from the rejected gate forward; completed stages before the gate are preserved |
| Unrecoverable schema conflict | Add a corrective Flyway migration (append-only) — never drop Flyway-managed tables |

---

## 7. Policy Guardrails

| Policy | Enforcement |
|--------|-------------|
| **Security** | No URL scheme other than `http://` or `https://` accepted (Jakarta Validation `@Pattern`). Rate limiting on all endpoints (Bucket4j). |
| **Compliance** | All schema changes go through Flyway migrations only — no Hibernate DDL auto-create in any profile. |
| **Change control** | Brownfield changes required impact analysis doc before implementation (see Phase 5). |
| **Test gate** | Implementation cannot be considered complete until tests pass (enforced by agent: did not write docs until 36/36 green). |
| **Human oversight** | No high-impact decision (stack choice, architecture, scope of ambiguous requirement) was made without a human approval checkpoint. |

---

## 8. Reliability Metrics

| Metric | Value |
|--------|-------|
| Stages completed | 7 / 8 (Phase 8 in progress) |
| Stage success rate | 100% (all completed stages passed their exit gates) |
| Compilation retries | 1 (Java version mismatch — Java 21 → 17) |
| Test retries | 2 (Mockito strictness fix; column length + placeholder collision fix) |
| Schema retries | 1 (VARCHAR(20) → VARCHAR(50) + sequence migration added) |
| Human approval events | 3 confirmed, 2 pending |
| End-to-end latency | ~35 minutes from assignment PDF read to 36/36 tests green |
| MTTR (mean time to fix a failure) | ~3 minutes average across all retries |
| Rollbacks | 0 (no stage was rolled back; all issues resolved by targeted fix) |

---

## 9. Dynamic Re-Planning Evidence

One instance of upstream-triggered re-planning occurred:

**Trigger**: Short code generation strategy (placeholder-row approach) caused unique constraint violations in integration tests.  
**Impact assessment**: `UrlShortenerService.generateUniqueCode()` only — no other modules affected.  
**Re-plan**: Add `V3__add_short_code_sequence.sql` Flyway migration, rewrite `generateUniqueCode()` to use DB sequence, update unit test mock for `EntityManager`.  
**Result**: All 36 tests green. No downstream stages invalidated.

---

## 10. Agent Autonomy Boundaries

| Within agent autonomy | Requires human |
|----------------------|----------------|
| Writing all code, tests, docs | Stack & architecture choices |
| Fixing compilation errors | Scope decisions on ambiguous requirements |
| Fixing test failures | Approving brownfield impact analysis |
| Adding Flyway migrations | Final sign-off on deliverables |
| Choosing implementation patterns | Rejecting or redirecting any major decision |
