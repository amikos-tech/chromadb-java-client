---
phase: 6
slug: tech-debt-cleanup
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 |
| **Config file** | pom.xml (surefire plugin) |
| **Quick run command** | `mvn test -Dtest=Phase06TechDebtValidationTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~60 seconds (quick) / ~180 seconds (full) |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=Phase06TechDebtValidationTest`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | DOC-BUG-1 | unit/static | `mvn test -Dtest=Phase06TechDebtValidationTest#test_readme_v2_hf_example_uses_withparam` | ❌ W0 | ⬜ pending |
| 06-01-02 | 01 | 1 | DOC-BUG-2 | unit/static | `mvn test -Dtest=Phase06TechDebtValidationTest#test_readme_sha256_description_accurate` | ❌ W0 | ⬜ pending |
| 06-01-03 | 01 | 1 | ASSUME-WIRE | unit/static | `mvn test -Dtest=Phase06TechDebtValidationTest#test_assume_min_version_has_callers` | ❌ W0 | ⬜ pending |
| 06-02-01 | 02 | 1 | INFRA-1 | unit/static | `mvn test -Dtest=Phase06TechDebtValidationTest#test_release_yml_no_branches_filter` | ❌ W0 | ⬜ pending |
| 06-02-02 | 02 | 1 | INFRA-2 | unit/static | `mvn test -Dtest=Phase06TechDebtValidationTest#test_release_yml_has_release_check_step` | ❌ W0 | ⬜ pending |
| 06-02-03 | 02 | 1 | ND4J-BUMP | unit/static | `mvn test -Dtest=Phase06TechDebtValidationTest#test_nd4j_version_is_m2_1` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/test/java/tech/amikos/chromadb/Phase06TechDebtValidationTest.java` — stubs for DOC-BUG-1, DOC-BUG-2, INFRA-1, INFRA-2, ASSUME-WIRE, ND4J-BUMP assertions

*Existing Phase05DocumentationReleaseReadinessTest.java covers some README/release.yml checks, but Phase 6-specific assertions belong in a dedicated file.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| v1 HuggingFace README examples | DOC-BUG-1 (v1 section) | Requires judgment on whether v1 API is still accessible | Grep for v1 HF constructor; if removed, mark section as legacy |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
