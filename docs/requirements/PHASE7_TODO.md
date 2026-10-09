# Phase 7 — Orchestration Layer (Agentic Workflow Engine)

**Status**: PENDING
**Gate Entry**: All three scenarios complete (Phases 2–6 cleared).
**Gate Exit**: Human reviews orchestration engine design and runtime behaviour.

---

## Overview
This is the critical differentiator. Build a real, in-process agentic orchestration engine
that manages the SDLC workflow as a stateful dependency graph — not a linear task chain.

The engine is exposed via REST so the human can inspect state, approve gates, and trigger rollback.

---

## Sub-Tasks

### 7.1 Dependency Graph Model
- [ ] `WorkflowStage.java` — enum of all SDLC stages (REQUIREMENTS, ARCHITECTURE, IMPLEMENTATION, TESTING, DOCS, BROWNFIELD, AMBIGUOUS, RELEASE)
- [ ] `StageStatus.java` — enum (PENDING, IN_PROGRESS, AWAITING_APPROVAL, APPROVED, FAILED, ROLLED_BACK, SKIPPED)
- [ ] `StageNode.java` — stage + status + dependencies + entry/exit conditions
- [ ] `WorkflowGraph.java` — directed acyclic graph, topological sort, cycle detection
- [ ] `WorkflowContext.java` — cross-stage shared state (key-value store with typed access)
- [ ] `DecisionRecord.java` — audit entry (timestamp, stage, decision, actor, rationale)

### 7.2 Workflow Engine Core
- [ ] `WorkflowEngine.java` — main engine class
  - `start(workflowId)` — initialize graph from definition
  - `advance(stageId)` — transition stage to IN_PROGRESS
  - `complete(stageId, outputs)` — mark complete, persist outputs to context
  - `fail(stageId, reason)` — trigger retry or rollback
  - `approve(stageId, approver)` — human approval → unblock dependent stages
  - `rollback(stageId)` — undo stage, mark dependents PENDING
  - `safeStop()` — gracefully halt at next gate
- [ ] Parallel stage execution: stages with no mutual dependency run concurrently (CompletableFuture)
- [ ] Sequential stages: wait for all dependencies before starting

### 7.3 Gate System
- [ ] `GatePolicy.java` — interface: `boolean canEnter(StageNode, WorkflowContext)` and `boolean canExit(StageNode, WorkflowContext)`
- [ ] `HumanApprovalGate.java` — blocks until `approve()` is called via API
- [ ] `AutomatedGate.java` — passes if all required outputs present in context
- [ ] `PolicyGate.java` — checks security/compliance rules (e.g., no test failures before deploy)

### 7.4 Retry, Fallback, Rollback, Safe-Stop
- [ ] `RetryPolicy.java` — max attempts, backoff strategy (exponential)
- [ ] `FallbackHandler.java` — per-stage fallback action (e.g., skip analytics on DB failure)
- [ ] `RollbackHandler.java` — per-stage rollback action (e.g., drop Flyway migration)
- [ ] `SafeStopController.java` — sets a flag; engine completes current stage then halts

### 7.5 Policy Guardrails
- [ ] `SecurityPolicy.java` — block stages if known vulnerabilities detected (placeholder hook)
- [ ] `CompliancePolicy.java` — block release if test coverage < 80%
- [ ] `ChangeControlPolicy.java` — require human approval before any DB migration stage

### 7.6 Audit & Observability
- [ ] `AuditLog.java` — append-only list of DecisionRecords, persisted to `reports/audit-log.json`
- [ ] `WorkflowMetrics.java` — tracks: stageSuccessRate, retryCount, rollbackCount, MTTR, e2e latency
- [ ] `WorkflowStateSnapshot.java` — serializable snapshot of full graph state (for replay/debug)

### 7.7 Dynamic Re-planning
- [ ] `RePlanningEngine.java` — on upstream output change: invalidate downstream stages, re-evaluate entry conditions, rebuild execution order
- [ ] Example trigger: architecture changed after tests written → tests marked PENDING, re-run required

### 7.8 Orchestration REST API
- [ ] `WorkflowController.java`
  - GET  /api/v1/workflow/status — full graph state
  - GET  /api/v1/workflow/stages/{stageId} — single stage detail
  - POST /api/v1/workflow/stages/{stageId}/approve — human approval
  - POST /api/v1/workflow/stages/{stageId}/rollback — trigger rollback
  - POST /api/v1/workflow/safe-stop — graceful halt
  - GET  /api/v1/workflow/audit — full audit log
  - GET  /api/v1/workflow/metrics — reliability metrics

---

## Outputs
- Full orchestration engine under `src/main/java/com/urlshortener/orchestrator/`
- `reports/audit-log.json` (populated at runtime)
- `docs/architecture/orchestration-design.md` — design narrative
