# Pitfalls Research

**Domain:** Java client SDK for Chroma vector database
**Researched:** 2026-03-17
**Confidence:** HIGH

## Critical Pitfalls

### Pitfall 1: API Drift Across Chroma Versions

**What goes wrong:**
Client assumptions stop matching server behavior, producing silent incompatibilities or parse failures.

**Why it happens:**
Endpoints evolve while client-side DTOs and tests lag behind.

**How to avoid:**
Maintain version-matrix integration tests and contract tests for endpoint payloads.

**Warning signs:**
Increasing integration test skips, frequent parsing hotfixes, unexplained 4xx/5xx in unchanged code paths.

**Phase to address:**
Phase 2 (API Coverage Completion)

---

### Pitfall 2: Inconsistent Authentication Application

**What goes wrong:**
Some endpoints work while others fail with auth errors due to missing headers in specific paths.

**Why it happens:**
Auth injection is not centralized or not covered by endpoint-wide tests.

**How to avoid:**
Enforce auth provider application in a single transport boundary and test all endpoint groups.

**Warning signs:**
401/403 failures only on specific operations, environment-dependent auth behavior.

**Phase to address:**
Phase 1 (Transport & Auth Hardening)

---

### Pitfall 3: Embedding Resolution Ambiguity

**What goes wrong:**
Different embedding sources are chosen unexpectedly, causing mismatched vector dimensions or provider calls.

**Why it happens:**
Fallback precedence between runtime embedding function and descriptor configuration is unclear.

**How to avoid:**
Codify precedence rules, fail fast on unsupported descriptors, and test conflict scenarios explicitly.

**Warning signs:**
Dimension mismatch errors, provider invocation surprises, non-deterministic query results.

**Phase to address:**
Phase 3 (Embedding & ID Extensibility)

---

### Pitfall 4: Weak Release Guardrails

**What goes wrong:**
A release ships with broken examples, missing metadata, or compatibility regressions.

**Why it happens:**
Docs/tests/release steps are maintained manually without enforceable gates.

**How to avoid:**
Automate release checklist checks and keep compatibility tests mandatory in CI.

**Warning signs:**
Post-release bugfix spikes, failed consumer onboarding, repeated release-script drift.

**Phase to address:**
Phase 5 (Documentation & Release Readiness)

---

### Pitfall 5: Java Baseline Regression

**What goes wrong:**
New changes accidentally use APIs not available in Java 8, breaking consumers.

**Why it happens:**
Local development uses newer JDKs without compatibility enforcement.

**How to avoid:**
Keep compiler/source target pinned to 1.8 and run compatibility checks in CI.

**Warning signs:**
Consumer runtime errors (`NoSuchMethodError`, class version issues), CI mismatch by JDK version.

**Phase to address:**
Phase 4 (Test Matrix & Compatibility)

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Skipping integration tests for new endpoints | Faster merge | Hidden API drift | Only for temporary red CI with tracked follow-up |
| Using generic runtime exceptions | Less code | Poor debuggability for users | Never for public API paths |
| Hardcoding provider assumptions | Quick implementation | Fragile multi-provider behavior | Never for shared resolver logic |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Chroma Cloud auth | Assume one auth scheme fits all deployments | Keep provider abstractions and validate each mode |
| Embedding APIs | Ignore provider-specific limits/timeouts | Validate inputs and surface provider-specific failures |
| Testcontainers | Pin test image implicitly via local defaults | Parameterize and matrix-test explicit versions |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Excessive per-record HTTP requests | Slow ingestion, high overhead | Encourage batched operations in examples/tests | Medium-to-high ingestion workloads |
| Large payload serialization churn | CPU spikes and latency | Reuse DTO patterns and validate payload size boundaries | Large batch query/add workflows |
| Unbounded retries/timeouts | Stalled callers | Explicit timeout and retry policy controls | Unstable networks or overloaded servers |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Logging secrets/tokens in request traces | Credential leakage | Redact auth headers and sensitive fields in logs |
| Failing open on auth config | Unauthorized access attempts or silent misrouting | Fail fast when required auth settings are missing |
| Overly broad exception messages from server responses | Information disclosure | Normalize public exception messaging while preserving diagnostics |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Ambiguous builder validation errors | Slow debugging, poor trust | Explicit field-level validation messages |
| Hidden defaults not documented | Surprising runtime behavior | Document defaults near API examples |
| Incomplete migration examples for v2 | Adoption friction | Provide minimal and advanced v2 code paths in README |

## "Looks Done But Isn't" Checklist

- [ ] **Auth support:** all endpoint families verify header propagation, not just health/list calls
- [ ] **API parity:** includes optional query/get parameters and edge-case error responses
- [ ] **Embedding support:** resolver precedence and unsupported-provider behavior are tested
- [ ] **Release readiness:** docs, versioning, signing, and compatibility checks pass in clean environment

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| API drift | MEDIUM | Add failing contract test, patch DTO/paths, backfill matrix coverage |
| Auth inconsistency | LOW-MEDIUM | Centralize auth injection and add endpoint-wide regression tests |
| Release guardrail gap | MEDIUM | Freeze release, patch checklist automation, re-run full pipeline |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| API drift across versions | Phase 2 | Version matrix integration + contract tests pass |
| Inconsistent auth application | Phase 1 | Auth regression tests pass for all endpoint groups |
| Embedding resolution ambiguity | Phase 3 | Resolver precedence tests pass with explicit conflict cases |
| Weak release guardrails | Phase 5 | Release checklist runs green in CI |
| Java baseline regression | Phase 4 | Java 8 compatibility and public interface tests pass |

## Sources

- Existing test strategy and architecture notes in `CLAUDE.md`
- Feature and TODO context from `README.md`
- Current v2 package and test files in repository

---
*Pitfalls research for: Java ChromaDB client SDK*
*Researched: 2026-03-17*
