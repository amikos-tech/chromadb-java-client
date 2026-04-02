---
gsd_state_version: 1.0
milestone: v1.5
milestone_name: milestone
status: "Phase 04 shipped — PR #146"
stopped_at: Completed 04-04-PLAN.md (BM25 & ChromaCloudSplade sparse providers)
last_updated: "2026-04-02T07:03:38.118Z"
progress:
  total_phases: 14
  completed_phases: 13
  total_plans: 36
  completed_plans: 36
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-17)

**Core value:** Java developers can integrate Chroma quickly and safely with a predictable, strongly-typed client that behaves consistently across environments.
**Current focus:** Phase 06 — documentation-site

## Current Position

Phase: 07
Plan: Not started

## Performance Metrics

**Velocity:**

- Total plans completed: 3
- Average duration: ~25 min
- Total execution time: ~1.3 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1. Transport & Auth Hardening | 3 | ~75 min | ~25 min |

**Recent Trend:**

- Last 3 plans: 01-01, 01-02, 01-03
- Trend: Stable

*Updated after each plan completion*
| Phase 02-api-coverage-completion P02 | 10min | 2 tasks | 5 files |
| Phase 02-api-coverage-completion P01 | 16min | 2 tasks | 3 files |
| Phase 02-api-coverage-completion P03 | 8min | 3 tasks | 4 files |
| Phase 03-embeddings-id-extensibility P01 | 20min | 2 tasks | 13 files |
| Phase 03-embeddings-id-extensibility PP03 | 28min | 2 tasks | 4 files |
| Phase 03-embeddings-id-extensibility P02 | 5min | 2 tasks | 2 files |
| Phase 04-compatibility-test-matrix PP01 | 15min | 3 tasks | 3 files |
| Phase 04-compatibility-test-matrix PP02 | 15 | 2 tasks | 2 files |
| Phase 05-documentation-release-readiness P01 | 5 | 2 tasks | 3 files |
| Phase 05-documentation-release-readiness P02 | 2 | 2 tasks | 2 files |
| Phase 06 P02 | 4 | 2 tasks | 2 files |
| Phase 06-tech-debt-cleanup P01 | 5 | 2 tasks | 3 files |
| Phase 07-readme-embedding-examples P01 | 3min | 2 tasks | 2 files |
| Phase 01-result-ergonomics-wheredocument P01 | 2min | 1 tasks | 7 files |
| Phase 01-result-ergonomics-wheredocument P03 | 5 | 2 tasks | 3 files |
| Phase 01-result-ergonomics-wheredocument P02 | 2 | 2 tasks | 6 files |
| Phase 02-collection-api-extensions P01 | 3 | 2 tasks | 7 files |
| Phase 02-collection-api-extensions P02 | 4 | 2 tasks | 6 files |
| Phase 05-cloud-integration-testing P01 | 4 | 4 tasks | 3 files |
| Phase 03-search-api P01 | 4 | 2 tasks | 12 files |
| Phase 03-search-api P02 | 3min | 2 tasks | 6 files |
| Phase 03-search-api P03 | 90 | 2 tasks | 7 files |
| Phase 05-cloud-integration-testing P02 | 4 | 2 tasks | 1 files |
| Phase 05 P03 | 5 | 1 tasks | 1 files |
| Phase 06-documentation-site P01 | 3 | 2 tasks | 24 files |
| Phase 06-documentation-site P03 | 7 | 2 tasks | 11 files |
| Phase 06-documentation-site P02 | 4 | 2 tasks | 12 files |
| Phase 06-documentation-site P04 | 5 | 1 tasks | 9 files |
| Phase 04-embedding-ecosystem P03 | 8min | 2 tasks | 9 files |
| Phase 04-embedding-ecosystem PP04 | 6min | 2 tasks | 12 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Repo-wide auth ambiguity is forbidden: exactly one auth strategy per builder.
- `defaultHeaders` must reject auth-conflicting keys and nudge users to `auth(...)`.
- Cloud preflight/identity auth failures are strict/typed (`ChromaUnauthorizedException`) with actionable safe hints.
- Mapping/auth contract changes require explicit tests and changelog entry.
- [Phase 02-api-coverage-completion]: Unknown config/schema payload is stored as immutable passthrough maps to prevent lossy round-trips.
- [Phase 02-api-coverage-completion]: Schema serialization is typed-authoritative: typed known keys override passthrough conflicts.
- [Phase 02-api-coverage-completion]: Unsupported CMEK provider entries are preserved in schema passthrough while known providers remain strictly validated.
- [Phase 02-api-coverage-completion]: createTenant/createDatabase are response-authoritative when response name is non-blank.
- [Phase 02-api-coverage-completion]: Fallback to request names is limited to create methods and not applied to get/list.
- [Phase 02-api-coverage-completion]: Mixed query inputs are accepted in either setter order; explicit queryEmbeddings are authoritative and queryTexts are embed fallback only.
- [Phase 02-api-coverage-completion]: Query include stays server-authoritative when omitted and is forwarded exactly when explicitly set by caller.
- [Phase 03-embeddings-id-extensibility P01]: ChromaException convenience constructors widened to public so embeddings.* packages can throw directly.
- [Phase 03-embeddings-id-extensibility P01]: EF precedence: explicit runtime > configuration.embedding_function > schema.default_embedding_function.
- [Phase 03-embeddings-id-extensibility P01]: WARNING log fires when explicit EF overrides persisted spec; FINE log fires on auto-wire from spec.
- [Phase 03-embeddings-id-extensibility]: Sha256IdGenerator throws IllegalArgumentException when both document AND metadata are null; empty metadata map is valid
- [Phase 03-embeddings-id-extensibility]: ChromaException is the boundary exception for all IdGenerator failures (null, blank, runtime exception) per EMB-04
- [Phase 03-embeddings-id-extensibility]: serializeMetadata uses TreeMap (sorted keys), key=value;key=value format, package-private for testability
- [Phase 03-embeddings-id-extensibility P02]: DefaultEmbeddingFunction download failures throw ChromaException (unchecked, v2) not EFException; validateModel() is stateless file-existence check (no static boolean flag)
- [Phase 03-embeddings-id-extensibility P02]: modelDownloadUrl is package-private non-final static for WireMock test injection without reflection
- [Phase 04]: Container startup failure throws AssertionError (fail-fast) not Assume.assumeTrue (silent skip) to make bad images visible
- [Phase 04]: GitHub Actions include pattern (not exclude) for JDK 11/17 x 1.5.5 cells produces exactly 5 cells with no accidental combinations
- [Phase 04]: CHROMA_MATRIX_VERSIONS := 1.0.0 1.3.7 1.5.5 centralized in Makefile; CI workflow maintains its own parallel matrix definition
- [Phase 04]: animal-sniffer check goal defaults to process-test-classes (runs during mvn test, not mvn compile) — no explicit phase override added per plan instructions
- [Phase 04]: EXPECTED_BUILDER_METHOD_COUNT=34 and EXPECTED_CLOUD_BUILDER_METHOD_COUNT=8 (getDeclaredMethods includes private methods for concrete classes; public-only counts would be 20 and 6)
- [Phase 05-documentation-release-readiness]: Auth provider examples use factory methods (BasicAuth.of(), TokenAuth.of(), ChromaTokenAuth.of()) - constructors are private
- [Phase 05-documentation-release-readiness]: CHANGELOG.md starts fresh at 0.2.0 - no backfill of 0.1.x history
- [Phase 05-documentation-release-readiness]: release-check artifact check is conditional on target/ directory presence to allow standalone doc validation
- [Phase 05-documentation-release-readiness]: release-dry-run uses mvn clean verify (not package) to produce sources/javadoc JARs and checksums
- [Phase 05-documentation-release-readiness]: release.yml integration test step uses Chroma 1.5.5 only as representative release gate to avoid 3x matrix overhead
- [Phase 06]: branches filter removed from release trigger: GitHub Actions ignores branches on release events
- [Phase 06]: release-check step inserted after Version bump, before Publish package to validate non-SNAPSHOT version
- [Phase 06]: nd4j-native-platform bumped from 1.0.0-M2 to 1.0.0-M2.1 (latest patch, no transitive conflicts)
- [Phase 06-tech-debt-cleanup]: v1 legacy HuggingFace example updated to WithParam.apiKey() because the bare-String constructor no longer exists — even legacy examples must reference the available constructor signature
- [Phase 06-tech-debt-cleanup]: testAssumeMinVersionSmokeTest uses assumeMinVersion(1.0.0) which always passes on all matrix versions, making it a safe non-inert wiring proof
- [Phase 07-readme-embedding-examples]: v1 Cohere stays minimal — apiKey only (WithParam.apiKey(apiKey)), no model param per locked Phase 07 context decision
- [Phase 07-readme-embedding-examples]: 8 WithParam import threshold (4 v2 + 4 v1) asserted in test to prevent future import drift
- [Phase 01-result-ergonomics-wheredocument]: ResultRow fields return null (not Optional) when Include not requested — consistent with existing GetResult/QueryResult approach
- [Phase 01-result-ergonomics-wheredocument]: QueryResultRowImpl uses composition (wraps ResultRowImpl) to avoid code duplication without inheritance
- [Phase 01-result-ergonomics-wheredocument]: Defensive copy applied on every getEmbedding() call, not just construction, to prevent aliasing across callers
- [Phase 01-result-ergonomics-wheredocument]: contains/notContains reject null and blank strings; regex/notRegex reject only null (empty string is valid regex)
- [Phase 01-result-ergonomics-wheredocument]: WhereDocument.contains() Javadoc clarifies distinction from Where.documentContains() per D-18: WhereDocument is local-compatible path, Where#documentContains is Cloud-oriented inline filter
- [Phase 01-result-ergonomics-wheredocument]: No no-arg rows() on QueryResult (per D-14): callers must always specify queryIndex to be explicit about which query group
- [Phase 01-result-ergonomics-wheredocument]: IntFunction anonymous class used in QueryResultImpl.stream() for Java 8 compatibility (avoids lambda syntax)
- [Phase 01-result-ergonomics-wheredocument]: Column-slice null-safe access: if a field list is null (not included), all rows return null for that field
- [Phase 02-collection-api-extensions]: forkCount() uses ForkCountResponse DTO (not Integer.class) because server returns {count: N} JSON object
- [Phase 02-collection-api-extensions]: fork() passes explicitEmbeddingFunction to from() for EF inheritance in forked collections (not null)
- [Phase 02-collection-api-extensions]: Cloud-only methods (fork, forkCount, indexingStatus) propagate ChromaNotFoundException naturally on 404 without special handling
- [Phase 02-collection-api-extensions]: IndexingStatus uses long fields (not int) for op counts matching Chroma API spec; no convenience isComplete() per D-11
- [Phase 02-collection-api-extensions]: TestContainers tests catch both ChromaNotFoundException and ChromaServerException for skip-on-unavailable — self-hosted returns 5xx for fork/indexingStatus not 404
- [Phase 02-collection-api-extensions]: Cloud fork test gated by CHROMA_RUN_FORK_TESTS=true to avoid per-call cloud cost in CI
- [Phase 05-cloud-integration-testing]: validateMetadataArrayTypes uses ChromaBadRequestException with typed errorCode strings (MIXED_TYPE_ARRAY, NULL_ARRAY_ELEMENT); Integer/Long normalized to Integer group, Float/Double to Float group for homogeneity
- [Phase 05-cloud-integration-testing]: Behavioral wiring tests for metadata validation use ChromaHttpCollection.from() with stub ChromaApiClient at localhost:1 — validation fires before network call
- [Phase 03-search-api]: Knn uses factory+fluent-chain (not inner Builder) because type is factory-discriminated by query type; single required parameter makes builder pattern overkill
- [Phase 03-search-api]: Rrf.Builder auto-calls knn.withReturnRank() on rank() to prevent returnRank=false pitfall in RRF sub-rankings
- [Phase 03-search-api]: SearchResult.getScores() uses List<List<Double>> (not Float) to match wire format precision
- [Phase 03-search-api]: SearchBuilderImpl in ChromaHttpCollection is stub throwing UnsupportedOperationException; full wiring in Plan 02
- [Phase 03-search-api]: SearchRequest.searches is List<Map<String,Object>> for polymorphic rank serialization (knn vs rrf)
- [Phase 03-search-api]: 'filter' key used (not 'where') in buildSearchItemMap per Search API wire format spec
- [Phase 03-search-api]: SearchResultImpl stores Double scores internally, downcasts to Float on row access per SearchResultRow contract
- [Phase 03-search-api]: RRF and text queryText skipped via Assume in integration tests — server returns 'unknown variant' for $rrf and rejects string values in $knn.query; tests document intended contract
- [Phase 03-search-api]: Wire format keys corrected to '$knn'/'$rrf' (dollar-prefixed) — bare 'knn'/'rrf' keys rejected by Chroma server
- [Phase 05-cloud-integration-testing]: CLOUD-01 search parity tests: GroupBy results via rows() only; ReadLevel WAL uses isolated collection without polling; RRF auto-skipped with Assume.assumeTrue false documenting server limitation; filter matrix 8 sub-scenarios inline; pagination client validation throws IllegalArgumentException before HTTP
- [Phase 05-cloud-integration-testing]: Embedding projection assertion loosened to accept null or [[null]]: server returns [[null]] for unselected embeddings
- [Phase 05-cloud-integration-testing]: WAL read-level test uses isolated 3D collection (col) instead of 4D seedCollection to avoid dimension mismatch
- [Phase 06-documentation-site]: java-examples/index.md placed inside docs_dir (docs/docs/java-examples/) — MkDocs only serves content from docs_dir; placing outside caused strict-mode build failure
- [Phase 06-documentation-site]: api/ nav entry uses api/index.md placeholder so mkdocs build --strict passes; actual Javadoc deployed by CI into api/ path over this placeholder
- [Phase 06-documentation-site]: maven-javadoc-plugin upgraded from 2.9.1 to 3.11.2 with doclint=none and source=8 for modern HTML5 Javadoc output and Java 8 compatibility
- [Phase 06-documentation-site]: Migration page uses pymdownx.tabbed for v1/v2 side-by-side examples — v1 code is the designated exception per D-11; all other pages are v2-only
- [Phase 06-documentation-site]: ChromaLoggers is package-private; logging docs expose only ChromaLogger interface and ChromaLogger.noop() as the public API surface
- [Phase 06-documentation-site]: SearchExample uses Search.builder() + collection.search().searches(...) pattern to expose full Knn/Rrf/Search type hierarchy
- [Phase 06-documentation-site]: All guide pages use --8<-- named section snippet inclusions (no inline copy-pasted code blocks) per D-09
- [Phase 06-documentation-site]: Examples stubs use 'coming soon' admonition with link to relevant guide page — Phase 7 fills content without touching nav config
- [Phase 06-documentation-site]: mkdocs.yml Examples nav uses section syntax with java-examples/index.md as section index per navigation.indexes feature
- [Phase 04-embedding-ecosystem]: Jackson version aligned to 2.17.2 via dependencyManagement to resolve nd4j/GenAI SDK conflict
- [Phase 04-embedding-ecosystem]: Voyage WireMock tests use WithParam.baseAPI() constructor injection instead of static field reflection
- [Phase 04-embedding-ecosystem]: Gemini/Bedrock use lazy double-checked locking for SDK client init to avoid load at construction time
- [Phase 04-embedding-ecosystem]: englishStemmer class name is lowercase in snowball-stemmer 1.3.0.581.1
- [Phase 04-embedding-ecosystem]: BM25StopWords contains 179 NLTK English stop words (not 174); ChromaCloudSplade uses Bearer token auth

### Roadmap Evolution

- Phase 6 added: Tech Debt Cleanup (DOC-BUG-1, DOC-BUG-2, INFRA-1, INFRA-2, inert assumeMinVersion removal)
- Phase 6 added: Documentation Site — rich docs with API surfaces, examples, and feature guides (similar to chroma-go)
- Phase 7 added: Working Examples — full runnable examples for all major features (similar to chroma-go examples/)

### Pending Todos

None.

### Blockers/Concerns

- [RESOLVED by P02] Full `mvn test` run was interrupted after stalling on first-time ONNX model download — resolved by OkHttp download with 300s timeout in DefaultEmbeddingFunction.

## Session Continuity

Last session: 2026-04-01T12:59:42.478Z
Stopped at: Completed 04-04-PLAN.md (BM25 & ChromaCloudSplade sparse providers)
Resume file: None
