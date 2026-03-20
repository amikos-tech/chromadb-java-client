# Phase 5: Documentation & Release Readiness - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Ship a polished, repeatable release experience for users and maintainers. Refresh README with v2-first onboarding examples, remove v1 API code entirely, provide a migration/breaking-changes guide, create a changelog, and build a validated release flow that catches documentation and packaging gaps before Maven Central publish. This phase does not add new product capabilities or change client behavior.

</domain>

<decisions>
## Implementation Decisions

### README restructure
- Structure: v2-first with v1 examples in a collapsed appendix section at the bottom for reference
- v2 examples to include: quick start (connect, create collection, add, query with default embedding), auth (BasicAuth, TokenAuth, ChromaTokenAuth via `ChromaClient.builder().auth(...)`), cloud + transport options (`ChromaClient.cloud()`, SSL cert, timeouts, custom OkHttpClient)
- Schema/embeddings/ID generator examples: already partially documented in README — keep and polish but not a primary focus
- Features checklist and TODO section: consolidate both into a compact "Status" section listing what's supported and what's planned
- Tone: professional confidence — lead with "Production-ready Java client for ChromaDB v2 API", remove "very basic/naive implementation" self-deprecation
- Remove stale references to unimplemented items (PaLM, Sentence Transformers, Cloudflare Workers AI) unless they move to the Status/planned section

### v1→v2 migration path
- **Remove v1 source code entirely** — delete `tech.amikos.chromadb.Client`, `tech.amikos.chromadb.Collection`, and all other v1 classes. This is a v2-only milestone; clean break
- **Remove v1 test code entirely** — delete all v1 test classes. No v1 source or test code remains
- Create top-level `MIGRATION.md` as a breaking changes + v2 quick start guide
- MIGRATION.md content: list what was removed (v1 classes, methods, patterns), mapping table of v1→v2 equivalents, 2-3 before/after code snippets for common flows (connect, add, query), then pointer to README v2 examples
- README links to MIGRATION.md for users upgrading from 0.1.x

### Release validation gate
- `make release-check` Makefile target that validates locally before tagging:
  - **Version format**: pom.xml version must not contain `-SNAPSHOT`
  - **Artifact completeness**: `mvn package` produces main JAR, sources JAR, javadoc JAR, checksums (MD5 + SHA-512) — all present and non-empty
  - **Javadoc clean build**: javadoc generation completes without errors
  - **Documentation freshness**: README references correct version, CHANGELOG.md has entry for the release version, no stale TODO items
- `make release-dry-run` target: runs `mvn clean package -Dgpg.skip=true` + validation checks. Produces all artifacts locally without signing/deploying
- Update `release.yml` to run full test suite (unit + integration tests) before deploying to Maven Central — remove `-DskipTests`

### Changelog & versioning
- CHANGELOG.md at project root following Keep a Changelog format (keepachangelog.com): Added/Changed/Removed/Fixed sections per version
- GitHub Release description mirrors CHANGELOG.md content — both are maintained
- Start fresh at 0.2.0 — no backfill of 0.1.x history (prior history in git tags/GitHub releases)
- 0.2.0 release notes highlight: v2 API surface, breaking changes (v1 removal), compatibility (Java 8, Chroma 1.0.0–1.5.5), migration pointer (link to MIGRATION.md)

### Claude's Discretion
- Exact README section ordering and heading hierarchy within the v2-first structure
- Exact MIGRATION.md mapping table format and which code snippets to include
- Which v1 classes/packages to remove (Claude should identify the full v1 surface)
- Exact release-check validation script implementation
- Exact CHANGELOG.md 0.2.0 entry content (derived from git history and phase summaries)
- Whether to update `release.yml` actions versions (e.g., checkout@v3 → checkout@v4)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirement contracts
- `.planning/ROADMAP.md` — Phase 5 goal, success criteria, plan scaffolding (QLTY-03, QLTY-04)
- `.planning/REQUIREMENTS.md` — QLTY-03/QLTY-04 acceptance targets
- `.planning/PROJECT.md` — milestone constraints (v2-only, Java 8, sync API)

### Project conventions
- `CLAUDE.md` — repository architecture, test commands, publishing conventions

### Documentation targets
- `README.md` — current README with mixed v1/v2 content (primary restructure target)

### Release infrastructure
- `pom.xml` — Maven build: GPG signing, source/javadoc JARs, checksum plugin, nexus staging, version `0.2.0-SNAPSHOT`
- `.github/workflows/release.yml` — current release workflow: GPG + nexus staging, triggered by GitHub release creation, currently skips tests
- `.github/workflows/integration-test.yml` — CI test matrix (JDK 8/11/17 x Chroma versions)
- `Makefile` — existing `release-prepare`, `release`, `release-rollback` targets

### v1 API surface (removal targets)
- `src/main/java/tech/amikos/chromadb/Client.java` — v1 client class
- `src/main/java/tech/amikos/chromadb/Collection.java` — v1 collection class
- `src/main/java/tech/amikos/chromadb/` — v1 package (identify all classes for removal vs shared utilities)

### Prior phase context
- `.planning/phases/01-transport-auth-hardening/01-CONTEXT.md` — auth contract decisions (one auth per builder, fail-fast)
- `.planning/phases/02-api-coverage-completion/02-CONTEXT.md` — API parity decisions
- `.planning/phases/03-embeddings-id-extensibility/03-CONTEXT.md` — embedding precedence, ID generators
- `.planning/phases/04-compatibility-test-matrix/04-CONTEXT.md` — version matrix, Java 8 enforcement, CI gating

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ChromaClient.builder()` and `ChromaClient.cloud()` — v2 entry points already documented in README sections that can be promoted to primary position
- v2 auth examples (BasicAuth, TokenAuth, ChromaTokenAuth) — partially documented in README "v2 Auth Contract" section
- v2 transport builder examples — documented in "v2 client builder transport options" section
- v2 schema/CMEK/queryTexts example — documented in "v2 Schema/CMEK/queryTexts" section
- v2 ID generators example — documented in "v2 ID generators" section
- Makefile already has `release-prepare`, `release-rollback`, `release`, `snapshot` targets

### Established Patterns
- pom.xml release plugins (GPG, source-jar, javadoc-jar, checksum, nexus-staging) are already configured and proven
- `release.yml` workflow uses `versions:set -DnewVersion=${{ github.ref_name }}` for version bump from git tag
- Makefile uses `##@` section headers and `##` target descriptions for auto-generated help

### Integration Points
- README restructure touches only documentation — no code behavior changes
- v1 code removal must be validated against v2 code to ensure no v2 classes import from v1 package
- `make release-check` integrates alongside existing Makefile release targets
- `release.yml` test addition needs Docker/Testcontainers support in the release runner (ubuntu-latest has Docker)
- CHANGELOG.md becomes a release-check validation input

</code_context>

<specifics>
## Specific Ideas

- The user wants a clean break: v1 code (source + tests) removed entirely, not deprecated
- README should lead with confidence: "Production-ready Java client for ChromaDB v2 API"
- MIGRATION.md is framed as "breaking changes + quick start" rather than a traditional migration guide
- Release flow should be locally verifiable (`make release-check`, `make release-dry-run`) before tagging
- CHANGELOG starts fresh at 0.2.0 — no archaeological backfill of 0.1.x history

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within Phase 5 scope.

</deferred>

---

*Phase: 05-documentation-release-readiness*
*Context gathered: 2026-03-20*
