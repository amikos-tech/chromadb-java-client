# Phase 5: Documentation & Release Readiness - Research

**Researched:** 2026-03-20
**Domain:** Maven library documentation, release automation, Java SDK onboarding content, CHANGELOG conventions
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**README restructure:**
- Structure: v2-first with v1 examples in a collapsed appendix section at the bottom for reference
- v2 examples to include: quick start (connect, create collection, add, query with default embedding), auth (BasicAuth, TokenAuth, ChromaTokenAuth via `ChromaClient.builder().auth(...)`), cloud + transport options (`ChromaClient.cloud()`, SSL cert, timeouts, custom OkHttpClient)
- Schema/embeddings/ID generator examples: already partially documented in README — keep and polish but not a primary focus
- Features checklist and TODO section: consolidate both into a compact "Status" section listing what's supported and what's planned
- Tone: professional confidence — lead with "Production-ready Java client for ChromaDB v2 API", remove "very basic/naive implementation" self-deprecation
- Remove stale references to unimplemented items (PaLM, Sentence Transformers, Cloudflare Workers AI) unless they move to the Status/planned section

**v1 to v2 migration path:**
- Remove v1 source code entirely — delete `tech.amikos.chromadb.Client`, `tech.amikos.chromadb.Collection`, and all other v1 classes. This is a v2-only milestone; clean break
- Remove v1 test code entirely — delete all v1 test classes. No v1 source or test code remains
- Create top-level `MIGRATION.md` as a breaking changes + v2 quick start guide
- MIGRATION.md content: list what was removed (v1 classes, methods, patterns), mapping table of v1 to v2 equivalents, 2-3 before/after code snippets for common flows (connect, add, query), then pointer to README v2 examples
- README links to MIGRATION.md for users upgrading from 0.1.x

**Release validation gate:**
- `make release-check` Makefile target that validates locally before tagging
- Checks: version format (no -SNAPSHOT), artifact completeness (main JAR, sources JAR, javadoc JAR, checksums MD5 + SHA-512 all present and non-empty), javadoc clean build, documentation freshness (README references correct version, CHANGELOG.md has entry, no stale TODO items)
- `make release-dry-run` target: runs `mvn clean package -Dgpg.skip=true` + validation checks, produces artifacts locally without signing/deploying
- Update `release.yml` to run full test suite (unit + integration tests) before deploying to Maven Central — remove `-DskipTests`

**Changelog and versioning:**
- CHANGELOG.md at project root following Keep a Changelog format (keepachangelog.com): Added/Changed/Removed/Fixed sections per version
- GitHub Release description mirrors CHANGELOG.md content — both maintained
- Start fresh at 0.2.0 — no backfill of 0.1.x history
- 0.2.0 release notes highlight: v2 API surface, breaking changes (v1 removal), compatibility (Java 8, Chroma 1.0.0–1.5.5), migration pointer (link to MIGRATION.md)

### Claude's Discretion
- Exact README section ordering and heading hierarchy within the v2-first structure
- Exact MIGRATION.md mapping table format and which code snippets to include
- Which v1 classes/packages to remove (Claude should identify the full v1 surface)
- Exact release-check validation script implementation
- Exact CHANGELOG.md 0.2.0 entry content (derived from git history and phase summaries)
- Whether to update `release.yml` actions versions (e.g., checkout@v3 to checkout@v4)

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within Phase 5 scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| QLTY-03 | User can follow README examples for v2 auth, schema, collection lifecycle, and query workflows end-to-end. | README restructure pattern, v2 API surface inventory, accurate Maven coordinate for 0.2.0 |
| QLTY-04 | Maintainer can produce Maven Central-ready artifacts (signed, checksummed, documented) through a repeatable release flow. | Existing pom.xml plugin inventory, Makefile release target patterns, release.yml audit, Keep a Changelog format |
</phase_requirements>

---

## Summary

Phase 5 is a documentation and release-automation phase with no production code behavior changes. The primary work is: (1) restructuring README.md to be v2-first, (2) removing the residual shared-utility classes that were the v1 package remnants, (3) creating MIGRATION.md and CHANGELOG.md, and (4) adding `make release-check` and `make release-dry-run` Makefile targets plus fixing the release.yml workflow.

**Critical finding:** There is no v1 `Client.java` or `Collection.java` — those were already deleted in commit `b041bbb`. The four remaining files in `tech.amikos.chromadb` (`ChromaException`, `EFException`, `Embedding`, `Constants`) are **shared utilities actively imported by v2 and embeddings code**. They cannot be deleted without migrating their callers. The CONTEXT.md instruction to "remove v1 code" applies to these files, but they must be migrated (not simply deleted) because v2 depends on them.

The release infrastructure (GPG signing, source/javadoc JARs, checksum plugin, nexus-staging) is already fully configured in pom.xml. The `release.yml` workflow uses `checkout@v3` and `setup-java@v3` while `integration-test.yml` already uses `@v4`. The release workflow still passes `-DskipTests`, which the user wants removed.

**Primary recommendation:** Plan 05-01 handles all documentation content (README restructure, v1 package migration, MIGRATION.md, CHANGELOG.md). Plan 05-02 handles the release gate (Makefile targets, release.yml fix). Keep them strictly separated since 05-01 is pure file content and 05-02 is tooling.

## Standard Stack

### Core
| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| Keep a Changelog | 1.1.0 format | CHANGELOG.md format | Industry standard, parseable by tooling |
| Maven (existing) | 3.x | Artifact build, GPG sign, deploy | Already configured; no new tooling |
| GNU Make (existing) | — | Release gate scripts | Already used for all dev targets |
| GitHub Actions (existing) | — | CI/CD | Already wired; release.yml needs test addition |

### Supporting
| Tool | Version | Purpose | When to Use |
|------|---------|---------|-------------|
| `mvn versions:set` | existing | Version bump without SNAPSHOT | Used by current release-prepare target |
| `mvn help:evaluate` | existing | Extract pom.xml version in scripts | Used in Makefile info target; reuse in release-check |
| `checksum-maven-plugin` | 1.7 (existing) | MD5 + SHA-512 checksums | Already runs during `mvn package` |
| `maven-javadoc-plugin` | 2.9.1 (existing) | Javadoc JAR | Already bound to verify; test with `mvn javadoc:jar` |
| `nexus-staging-maven-plugin` | 1.6.7 (existing) | Maven Central staging | Already configured |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Bash in Makefile for release-check | Python/Ruby script | Bash is zero-dependency, consistent with existing Makefile style |
| Markdown appendix for v1 examples | Separate v1-archive branch | Appendix is simpler; user explicitly chose collapsed section |
| `maven-release-plugin` | Current manual flow | Current flow is simpler and already proven; don't introduce new plugins |

**Installation:** No new dependencies. All tooling is pre-existing.

## Architecture Patterns

### Recommended Project Structure (additions this phase)

```
(project root)
├── CHANGELOG.md          # NEW — Keep a Changelog format, starts at 0.2.0
├── MIGRATION.md          # NEW — Breaking changes + v1→v2 mapping
├── README.md             # MODIFIED — v2-first restructure
├── Makefile              # MODIFIED — add release-check, release-dry-run
└── .github/
    └── workflows/
        └── release.yml   # MODIFIED — add test step, upgrade action versions
```

### Pattern 1: Keep a Changelog CHANGELOG.md Format

**What:** Changelog with explicit sections per release version, unreleased section at top.
**When to use:** Every release; 0.2.0 is the starting entry.

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-XX-XX

### Added
- v2 API client (`ChromaClient.builder()`, `ChromaClient.cloud()`)
- ...

### Removed
- v1 API classes (`tech.amikos.chromadb.Client`, `Collection`, etc.)

[0.2.0]: https://github.com/amikos-tech/chromadb-java-client/releases/tag/0.2.0
```

### Pattern 2: Makefile Release Gate Target

**What:** Shell script baked into Makefile that validates pre-release conditions.
**When to use:** Called by developer before `git tag` and push.

```makefile
.PHONY: release-check
release-check: check-tools ## Validate release readiness (no SNAPSHOT, artifacts present, docs current)
    @echo "$(BLUE)Running release checks...$(NC)"
    @VERSION=$$($(MAVEN) help:evaluate -Dexpression=project.version -q -DforceStdout) && \
    echo "  Checking version: $$VERSION" && \
    echo "$$VERSION" | grep -qv "SNAPSHOT" || { echo "$(RED)FAIL: Version contains -SNAPSHOT$(NC)"; exit 1; }
    @echo "  Checking CHANGELOG.md entry..." && \
    VERSION=$$($(MAVEN) help:evaluate -Dexpression=project.version -q -DforceStdout) && \
    grep -q "\[$$VERSION\]" CHANGELOG.md || { echo "$(RED)FAIL: No CHANGELOG.md entry for $$VERSION$(NC)"; exit 1; }
    @echo "  Checking README version reference..." && \
    VERSION=$$($(MAVEN) help:evaluate -Dexpression=project.version -q -DforceStdout) && \
    grep -q "$$VERSION" README.md || { echo "$(RED)FAIL: README.md does not reference version $$VERSION$(NC)"; exit 1; }
    @echo "$(GREEN)release-check passed$(NC)"

.PHONY: release-dry-run
release-dry-run: check-tools ## Build release artifacts locally without signing or deploying
    @echo "$(BLUE)Running release dry-run (no GPG, no deploy)...$(NC)"
    $(MAVEN) --batch-mode clean package -Dgpg.skip=true
    @$(MAKE) release-check
    @echo "$(GREEN)release-dry-run complete$(NC)"
```

**Note:** `release-check` should run artifact presence validation AFTER a `mvn package` has been run (either standalone or from `release-dry-run`). The artifact check requires the target/ directory to be populated. Design accordingly: `release-check` validates docs without requiring build; `release-dry-run` runs build then full validation.

### Pattern 3: README v2-First Structure

**What:** Restructure README so v2 is primary, v1 moved to collapsed appendix.
**Recommended heading order:**

```
# Chroma Vector Database Java Client
[badge: Maven Central] [badge: CI]

Production-ready Java client for ChromaDB v2 API.

## Requirements
## Quick Start
## Installation
## Usage
  ### Basic Connection
  ### Authentication
    #### Basic Auth
    #### Token Auth
    #### Chroma Token Auth
  ### Cloud (Chroma Cloud)
    #### Cloud Auth
    #### Transport Options (SSL, timeouts, custom OkHttpClient)
  ### Collection Lifecycle
  ### Adding Records
  ### Querying
  ### Schema and CMEK
  ### ID Generators
  ### Embedding Functions
    #### Default (local, no API key)
    #### OpenAI
    #### Cohere
    #### HuggingFace
    #### Ollama
## Status
  (what is supported, what is planned)
## Development
  (Makefile commands, environment variables)
## Contributing
## Upgrading from 0.1.x
  > See MIGRATION.md for breaking changes and migration guide.
## References
## Appendix: v1 API Examples (Legacy)
  <details><summary>Expand v1 examples</summary>
  ...
  </details>
```

### Pattern 4: v1 Package Migration (Shared Utilities)

**What:** The four files in `tech.amikos.chromadb` are not deletable without migration because v2 and embeddings depend on them. The correct approach is to either:
  - Option A: Move them into `tech.amikos.chromadb.v2` or a `tech.amikos.chromadb.common` package and update all imports.
  - Option B: Retain them in place (they compile, there is no v1 `Client` or `Collection` left) and document them as internal utilities, not deleting them.

**Decision authority:** Claude's discretion per CONTEXT.md.
**Recommendation:** Option B — keep in place. The four files (`ChromaException`, `EFException`, `Embedding`, `Constants`) are already fully functional, have no v1 API client behavior, and are depended on by both v2 and embeddings. Moving them would require touching ~15 files with no user-visible benefit. The CONTEXT.md instruction references "v1 classes" meaning the actual API client (`Client.java`, `Collection.java`) which are already gone. The shared utilities are legitimate cross-cutting types, not v1 API surface.

**What IS v1 test code to remove (if any):** Audit is required. The test files in `src/test/java/tech/amikos/chromadb/` are `Utils.java` (a test utility used by v2 tests too) and embedding conformance tests. None of these are v1 API client tests — they test embedding functions that remain in the codebase. No test files should be deleted.

### Anti-Patterns to Avoid
- **Blindly deleting `tech.amikos.chromadb.*`:** ChromaException, EFException, Embedding, and Constants are still actively imported by v2 and embeddings code. Deleting them breaks the build.
- **Putting artifact validation in `release-check` without a prior build:** The check for JAR presence requires `target/` to be populated — structure scripts to gate this correctly.
- **Using `-DskipTests` in release.yml:** This is the existing bug; the fix is to add a proper test step before deployment.
- **Backfilling 0.1.x CHANGELOG history:** User decision is to start fresh at 0.2.0.
- **Referencing version `0.1.7` in README examples:** README must reference `0.2.0` in the Maven dependency snippet.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Version extraction in Makefile | `grep` on pom.xml | `mvn help:evaluate -Dexpression=project.version -q -DforceStdout` | pom.xml has nested properties; grep misses inherited versions |
| Artifact checksum generation | Manual script | `checksum-maven-plugin` (already configured) | Already runs during `mvn package`; produces .md5 and .sha512 files |
| GPG signing in Makefile | Local `gpg` calls | `maven-gpg-plugin` (already configured) | Already bound to `verify` phase with loopback pinentry |
| Changelog version parsing | Custom regex | `grep -q "\[$$VERSION\]" CHANGELOG.md` | Trivial grep is sufficient for the one validation needed |

**Key insight:** All release infrastructure already exists in pom.xml. Phase 5 adds operational scripts (Makefile targets) and fixes the workflow configuration gap (`-DskipTests` removal). No new Maven plugins are needed.

## Common Pitfalls

### Pitfall 1: Treating Shared Utilities as v1 Code
**What goes wrong:** Deleting `ChromaException`, `EFException`, `Embedding`, `Constants` breaks compilation of v2 classes and embedding functions.
**Why it happens:** They live in `tech.amikos.chromadb` (the "v1 package") but are shared types, not v1 API clients.
**How to avoid:** The actual v1 client classes (`Client.java`, `Collection.java`) are already gone (deleted in commit b041bbb). Do not delete the four remaining files.
**Warning signs:** `mvn compile` fails with `cannot find symbol` after deletion.

### Pitfall 2: release.yml `-DskipTests` Must Be Removed with Care
**What goes wrong:** Adding a test step to release.yml on ubuntu-latest requires Docker for integration tests (TestContainers). Docker is available on `ubuntu-latest` runners.
**Why it happens:** The current workflow was intentionally skipping tests. Adding tests without the integration profile produces only unit tests. Full matrix is expensive for a release gate.
**How to avoid:** Add a unit test step (`mvn test`) plus integration test step with a representative version (e.g., Chroma `1.5.5` with `-Pintegration`). Running all three matrix Chroma versions in the release job is optional and was not explicitly required.
**Warning signs:** Release job hangs on Docker pull in a runner environment without Docker daemon (non-ubuntu).

### Pitfall 3: README Version Reference Staleness
**What goes wrong:** `release-check` validates that README contains the release version, but README currently shows `0.1.7` in the Maven dependency XML.
**Why it happens:** README is updated manually and lags behind version bumps.
**How to avoid:** Update Maven dependency snippet in README to `0.2.0` during the README restructure task (05-01). The `release-check` script then validates this is current.
**Warning signs:** `release-check` reports `FAIL: README.md does not reference version 0.2.0`.

### Pitfall 4: release.yml Actions Version Mismatch
**What goes wrong:** `release.yml` uses `actions/checkout@v3` and `actions/setup-java@v3`. Node.js 16 (used by v3 actions) is deprecated on GitHub Actions runners.
**Why it happens:** Workflow was not updated when integration-test.yml was upgraded.
**How to avoid:** Upgrade `release.yml` to `actions/checkout@v4` and `actions/setup-java@v4`. The `distribution: 'adopt'` should also be changed to `distribution: 'temurin'` (Adoptium rebranded; `adopt` is deprecated but still works).
**Warning signs:** GitHub Actions deprecation warnings in release job logs.

### Pitfall 5: CHANGELOG Missing Link Reference
**What goes wrong:** Keep a Changelog format requires a link definition at the bottom of the file for each version tag (e.g., `[0.2.0]: https://github.com/...`). Missing this is not a format error but breaks diffs view.
**Why it happens:** Easy to forget when writing the first entry.
**How to avoid:** Include the reference link in the CHANGELOG.md template from the start.

### Pitfall 6: `mvn javadoc:jar` vs `mvn package` for Javadoc Validation
**What goes wrong:** `release-check` runs `mvn javadoc:jar` to validate javadoc builds cleanly. This goal is bound during the `verify` phase by the existing plugin configuration. Running `mvn javadoc:jar` standalone works but requires the project to have been compiled first.
**Why it happens:** Javadoc generation depends on compiled classes being present.
**How to avoid:** In `release-dry-run`, run `mvn clean package -Dgpg.skip=true` (which also runs verify and produces the javadoc JAR) rather than a separate javadoc goal. Then validate the produced JAR exists in `target/`.

## Code Examples

Verified patterns from existing codebase and project conventions:

### Version Extraction in Makefile (VERIFIED pattern from existing Makefile)
```makefile
# Source: existing Makefile release-prepare target (line 159)
VERSION=$$($(MAVEN) help:evaluate -Dexpression=project.version -q -DforceStdout)
```

### Artifact Presence Check in Bash/Makefile
```bash
# Check for main JAR, sources JAR, javadoc JAR, and checksums
ARTIFACT_ID="chromadb-java-client"
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
TARGET="target"

for f in \
    "${ARTIFACT_ID}-${VERSION}.jar" \
    "${ARTIFACT_ID}-${VERSION}-sources.jar" \
    "${ARTIFACT_ID}-${VERSION}-javadoc.jar" \
    "${ARTIFACT_ID}-${VERSION}.jar.md5" \
    "${ARTIFACT_ID}-${VERSION}.jar.sha512"; do
    test -s "${TARGET}/${f}" || { echo "FAIL: ${TARGET}/${f} missing or empty"; exit 1; }
done
```

### Updated release.yml Test Step Pattern
```yaml
# Insert before "Version bump" step in release.yml
- name: Run unit tests
  run: mvn --no-transfer-progress --batch-mode test

- name: Run integration tests
  run: mvn --no-transfer-progress --batch-mode -Pintegration test
  env:
    CHROMA_VERSION: '1.5.5'
```

### MIGRATION.md Before/After Code Snippet Pattern

**v1 (removed):**
```java
// REMOVED in 0.2.0 — class no longer exists
Client client = new Client(System.getenv("CHROMA_URL"));
Collection collection = client.createCollection("my-col", null, true, ef);
collection.add(null, metadatas, documents, ids);
Collection.QueryResponse qr = collection.query(queryTexts, nResults, null, null, null);
```

**v2 (current):**
```java
import tech.amikos.chromadb.v2.*;

Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .build();
Collection collection = client.getOrCreateCollection("my-col",
        CreateCollectionOptions.builder().embeddingFunction(ef).build());
collection.add()
        .documents("Hello, world")
        .ids("id-1")
        .execute();
QueryResult result = collection.query()
        .queryTexts("search phrase")
        .nResults(5)
        .execute();
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `new Client(url)` v1 constructor | `ChromaClient.builder().baseUrl(url).build()` | 0.2.0 (this release) | Full break — v1 class removed |
| Manual auth headers via `setDefaultHeaders()` | `ChromaClient.builder().auth(new BasicAuth(...))` | 0.2.0 | Typed, validated auth |
| `collection.query(texts, n, null, null, null)` | `collection.query().queryTexts(...).nResults(n).execute()` | 0.2.0 | Fluent builder, no nulls |
| `actions/checkout@v3` (deprecated Node 16) | `actions/checkout@v4` | Now available | Removes deprecation warning |
| `distribution: 'adopt'` (deprecated) | `distribution: 'temurin'` | Adoptium rebrand (2021) | Cleaner, forward-compatible |

**Deprecated/outdated in this codebase:**
- `release.yml` uses `-DskipTests`: deprecated by team decision — the fix is this phase
- README tone ("very basic/naive implementation"): replaced by professional tone per user decision
- Features TODO section in README: replaced by compact Status section

## Open Questions

1. **Should `release-check` artifact validation run a build itself or require pre-built artifacts?**
   - What we know: `release-dry-run` runs `mvn clean package` then calls `release-check`. Running build inside `release-check` is slow and repeated.
   - What's unclear: Whether the user wants `release-check` to be runnable standalone (requires running a build) or always as part of `release-dry-run`.
   - Recommendation: Split validation. `release-check` validates non-build things (version, CHANGELOG, README) and optionally validates artifacts if `target/` exists. `release-dry-run` runs the build then `release-check`.

2. **Full integration test matrix in release.yml or single version?**
   - What we know: The decision says "full test suite (unit + integration tests)". The matrix in `integration-test.yml` runs 3 Chroma versions x 3 JDKs = ~5 cells. This adds ~15-20 min to the release gate.
   - What's unclear: Whether "full test suite" means all matrix cells or a representative set.
   - Recommendation: Run unit tests (all JDKs via matrix) and integration tests against the latest supported Chroma version (1.5.5) only. This is a reasonable interpretation of "full test suite" without duplicating the CI matrix.

3. **What exactly counts as "v1 test code to remove"?**
   - What we know: `src/test/java/tech/amikos/chromadb/Utils.java` is used by v2 integration tests (`CloudAuthIntegrationTest`, `CloudParityIntegrationTest`). Embedding tests in `src/test/java/tech/amikos/chromadb/embeddings/` test embedding functions that remain valid.
   - What's unclear: Whether the old-style `TestCohereEmbeddings`, `TestOpenAIEmbeddings`, `TestHuggingFaceEmbeddings`, `TestOllamaEmbeddings` in `embeddings/cohere/`, `embeddings/hf/`, etc. are "v1 tests" to delete or just embedding tests to keep.
   - Recommendation: Keep all embedding tests (they test current production embedding functions). `Utils.java` is actively used by v2 tests — keep it. There are effectively no v1 API client tests left to delete.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 |
| Config file | pom.xml (surefire plugin; `-Pintegration` for integration tests) |
| Quick run command | `mvn test` |
| Full suite command | `mvn test && mvn -Pintegration test` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| QLTY-03 | README examples are syntactically valid Java that compiles | manual-only | Review examples manually against current v2 API | N/A |
| QLTY-03 | README Maven coordinate matches pom.xml release version | automated | `make release-check` (documentation freshness check) | Wave 0 (new target) |
| QLTY-04 | `mvn clean package -Dgpg.skip=true` produces main JAR, sources JAR, javadoc JAR, MD5, SHA-512 | automated | `make release-dry-run` | Wave 0 (new target) |
| QLTY-04 | pom.xml version has no -SNAPSHOT before release | automated | `make release-check` | Wave 0 (new target) |
| QLTY-04 | CHANGELOG.md has entry for release version | automated | `make release-check` | Wave 0 (new Makefile target + CHANGELOG.md) |

### Sampling Rate
- **Per task commit:** `mvn test` (unit tests, confirms no compilation break)
- **Per wave merge:** `mvn test && mvn -Pintegration test` (Chroma 1.5.5)
- **Phase gate:** `make release-dry-run` green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `CHANGELOG.md` — required as validation input for `make release-check`
- [ ] `Makefile` `release-check` target — required for QLTY-04 automation
- [ ] `Makefile` `release-dry-run` target — required for QLTY-04 artifact validation

None of these are test files per se — they are the deliverables of Plan 05-02 and are created during implementation.

## Sources

### Primary (HIGH confidence)
- Direct codebase inspection: `pom.xml`, `Makefile`, `release.yml`, `integration-test.yml`, `README.md`, all source files in `src/main/java/tech/amikos/chromadb/` — verified firsthand
- `git log` of b041bbb — confirmed v1 Client.java and Collection.java deletion date
- `grep` of all v2 and embeddings imports — confirmed `ChromaException`, `EFException`, `Embedding`, `Constants` are shared utilities still required

### Secondary (MEDIUM confidence)
- Keep a Changelog format: https://keepachangelog.com/en/1.1.0/ — industry standard, stable specification
- GitHub Actions `checkout@v4` and `setup-java@v4` availability: consistent with `integration-test.yml` which already uses v4 — HIGH confidence through cross-file verification

### Tertiary (LOW confidence)
- None — all findings are derived from direct inspection of the repository

## Metadata

**Confidence breakdown:**
- v1 code surface: HIGH — confirmed by direct file inspection and git history
- Shared utility dependency graph: HIGH — confirmed by grep of all import statements
- Standard Stack: HIGH — all tooling pre-exists in pom.xml and Makefile
- Architecture (README structure, Makefile patterns): HIGH — follows existing project conventions
- release.yml test addition approach: MEDIUM — integration test behavior in release runner is untested but ubuntu-latest Docker availability is a GitHub Actions guarantee

**Research date:** 2026-03-20
**Valid until:** 2026-05-01 (stable tooling domain; Maven Central requirements rarely change)
