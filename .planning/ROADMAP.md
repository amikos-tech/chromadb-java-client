# Roadmap: ChromaDB Java Client (Milestone 0.2.0 - v2 Only)

## Overview

This roadmap defines milestone `0.2.0` for a v2-only Java client release. It takes the existing v2 Java client from strong baseline to stable release readiness by first hardening transport/auth behavior, then closing API parity, then finalizing embedding determinism, and finally locking compatibility and release quality gates. Chroma v1 API support is explicitly out of scope.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Transport & Auth Hardening** - Ensure request/auth behavior is consistent and failure modes are actionable.
- [x] **Phase 2: API Coverage Completion** - Close remaining v2 lifecycle and record-operation parity gaps.
- [x] **Phase 3: Embeddings & ID Extensibility** - Make embedding resolution and ID generation deterministic and robust. (completed 2026-03-19)
- [ ] **Phase 4: Compatibility & Test Matrix** - Strengthen regression protection across Java and Chroma versions.
- [ ] **Phase 5: Documentation & Release Readiness** - Finish onboarding docs and repeatable Maven Central release flow.

## Phase Details

### Phase 1: Transport & Auth Hardening
**Goal**: Deliver endpoint-wide auth consistency and reliable exception semantics.
**Depends on**: Nothing (first phase)
**Requirements**: [AUTH-01, AUTH-02, AUTH-03, API-04]
**Success Criteria** (what must be TRUE):
  1. User can configure any supported auth provider and all client endpoints honor it.
  2. Cloud/preflight identity flows return expected typed results or explicit typed exceptions.
  3. Auth misconfiguration yields actionable validation or client errors without silent fallback.
  4. HTTP error responses are translated consistently into the correct `ChromaException` subclasses.
**Plans**: 3 plans

Plans:
- [x] 01-01: Audit and unify auth application across transport paths
- [x] 01-02: Harden error translation and auth-validation scenarios
- [x] 01-03: Expand auth-focused unit/integration coverage

### Phase 2: API Coverage Completion
**Goal**: Achieve complete and predictable v2 endpoint/operation parity required by current requirements.
**Depends on**: Phase 1
**Requirements**: [API-01, API-02, API-03]
**Success Criteria** (what must be TRUE):
  1. User can perform tenant/database lifecycle operations with typed request/response contracts.
  2. User can create and manage collections with schema/config/CMEK data preserved round-trip.
  3. User can execute add/get/query/update/upsert/delete with full filter/include/queryTexts behavior.
  4. Contract tests prove response mapping for the covered API surface.
**Plans**: 3 plans

Plans:
- [x] 02-01-PLAN.md — Lifecycle create fallback parity for tenant/database operations
- [x] 02-02-PLAN.md — Non-lossy schema/config/CMEK mapping and precedence hardening
- [x] 02-03-PLAN.md — Record query/include parity and two-version verification gate

### Phase 3: Embeddings & ID Extensibility
**Goal**: Ensure embedding and ID workflows are deterministic, extensible, and safe for production ingestion/query paths.
**Depends on**: Phase 2
**Requirements**: [EMB-01, EMB-02, EMB-03, EMB-04]
**Success Criteria** (what must be TRUE):
  1. User can use all supported embedding providers through one consistent contract.
  2. Default local embedding works reliably without external API credentials.
  3. Runtime and descriptor embedding precedence is deterministic and documented.
  4. UUID/ULID/SHA-256 generators work correctly with clear validation failures for invalid inputs.
**Plans**: 3 plans

Plans:
- [ ] 03-01-PLAN.md — Provider error normalization, conformance tests, and EF precedence documentation
- [ ] 03-02-PLAN.md — DefaultEmbeddingFunction download reliability (timeout, retry, thread safety)
- [ ] 03-03-PLAN.md — ID generator validation edges (SHA-256 metadata fallback, duplicate ID detection)

### Phase 4: Compatibility & Test Matrix
**Goal**: Reduce regression risk by enforcing Java baseline and Chroma-version compatibility expectations.
**Depends on**: Phase 3
**Requirements**: [QLTY-01, QLTY-02]
**Success Criteria** (what must be TRUE):
  1. Maintainer can run a reproducible matrix of unit/integration tests across supported Chroma versions.
  2. Java 8 compatibility checks prevent incompatible API/language usage from reaching release branches.
  3. Public interface compatibility tests detect breaking API changes before publication.
**Plans**: 2 plans

Plans:
- [ ] 04-01-PLAN.md — Version-matrix test infrastructure (assumeMinVersion helper, fail-fast startup, Makefile test-matrix, CI matrix workflow)
- [ ] 04-02-PLAN.md — Compatibility guardrails (animal-sniffer Java 8 enforcement, expanded PublicInterfaceCompatibilityTest)

### Phase 5: Documentation & Release Readiness
**Goal**: Ship a polished, repeatable release experience for users and maintainers.
**Depends on**: Phase 4
**Requirements**: [QLTY-03, QLTY-04]
**Success Criteria** (what must be TRUE):
  1. User can onboard using README v2 examples for auth, schema, lifecycle, and query flows.
  2. Maintainer can run release flow to produce signed, checksummed, publish-ready artifacts.
  3. Release checklist catches documentation and packaging gaps before publish.
**Plans**: 2 plans

Plans:
- [ ] 05-01: Refresh v2 documentation, examples, and migration guidance
- [ ] 05-02: Automate and verify release checklist and artifact validation

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Transport & Auth Hardening | 3/3 | Complete | 2026-03-18 |
| 2. API Coverage Completion | 3/3 | Complete | 2026-03-18 |
| 3. Embeddings & ID Extensibility | 3/3 | Complete   | 2026-03-19 |
| 4. Compatibility & Test Matrix | 0/2 | Not started | - |
| 5. Documentation & Release Readiness | 0/2 | Not started | - |
