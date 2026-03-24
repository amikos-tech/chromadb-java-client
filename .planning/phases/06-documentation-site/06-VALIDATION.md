---
phase: 6
slug: documentation-site
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-24
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | MkDocs build (strict mode) + Maven javadoc plugin |
| **Config file** | `docs/mkdocs.yml` |
| **Quick run command** | `mkdocs build --strict --config-file docs/mkdocs.yml` |
| **Full suite command** | `mkdocs build --strict --config-file docs/mkdocs.yml && mvn compile javadoc:javadoc -q` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mkdocs build --strict --config-file docs/mkdocs.yml`
- **After every plan wave:** Run `mkdocs build --strict --config-file docs/mkdocs.yml && mvn compile javadoc:javadoc -q`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | DOCS-BUILD | smoke | `mkdocs build --strict --config-file docs/mkdocs.yml` | ❌ W0 | ⬜ pending |
| 06-01-02 | 01 | 1 | DOCS-SNIPPETS | smoke | `mkdocs build --strict --config-file docs/mkdocs.yml` | ❌ W0 | ⬜ pending |
| 06-02-01 | 02 | 2 | DOCS-JAVADOC | smoke | `mvn compile javadoc:javadoc -q` | ✅ (pom.xml) | ⬜ pending |
| 06-03-01 | 03 | 2 | DOCS-WORKFLOW | lint | `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/docs.yml'))"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `docs/requirements.txt` — pip install target for CI
- [ ] `docs/mkdocs.yml` — MkDocs configuration
- [ ] `docs/docs/index.md` — homepage stub needed for any `mkdocs build` to succeed
- [ ] `docs/docs/assets/snippets/` — directory must exist; snippets plugin errors if base_path doesn't resolve

*These are scaffold files, not test files — they must exist before any build validation command can run.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Custom domain resolves | DOCS-DOMAIN | DNS propagation + Cloudflare config | Verify `java.chromadb.dev` loads after deploy |
| Visual theme matches chroma-go | DOCS-VISUAL | Subjective design review | Compare side-by-side with `go-client.chromadb.dev` |
| GA tracking fires | DOCS-ANALYTICS | Requires live deploy + GA dashboard | Deploy, visit pages, check GA Real-Time report |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
