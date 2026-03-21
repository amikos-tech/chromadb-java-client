# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `Collection.fork(String newName)` — copy-on-write collection fork (Chroma Cloud only)
- `Collection.forkCount()` — returns the number of forks for a collection (Chroma Cloud only)
- `Collection.indexingStatus()` — returns current indexing progress as `IndexingStatus` value object (Chroma Cloud only, Chroma >= 1.4.1)
- `IndexingStatus` immutable value object with `getNumIndexedOps()`, `getNumUnindexedOps()`, `getTotalOps()`, `getOpIndexingProgress()`
- Cloud integration tests for fork, forkCount, and indexingStatus (`CollectionApiExtensionsCloudTest`)
- TestContainers integration tests for fork, forkCount, and indexingStatus with auto-skip on 404/5xx (`CollectionApiExtensionsIntegrationTest`)
- Cloud vs Self-Hosted feature parity table in README covering all 30 v2 operations
- `<strong>Availability:</strong>` Javadoc tags on all v2 Collection and Client methods

## [0.2.0] - UNRELEASED

### Added

- v2 builder-based client API (`ChromaClient.builder()` for self-hosted, `ChromaClient.cloud()` for Chroma Cloud)
- Typed authentication providers: `BasicAuth`, `TokenAuth` (Bearer), `ChromaTokenAuth` (X-Chroma-Token header)
- Fluent record operations with builder pattern: `collection.add().documents(...).ids(...).execute()`
- Fluent query builder: `collection.query().queryTexts(...).nResults(n).include(...).execute()`
- Type-safe filter DSL via `Where` and `WhereDocument` classes
- Schema and CMEK support for collection configuration (`Schema.builder()`, `Cmek.gcpKms(...)`)
- Transport customization: SSL certificate injection, connect/read/write timeouts, custom `OkHttpClient`
- Environment variable convenience methods: `tenantFromEnv(...)`, `databaseFromEnv(...)`, `tenantAndDatabaseFromEnv()`
- ID generators for client-side ID generation: `UuidIdGenerator`, `UlidIdGenerator`, `Sha256IdGenerator`
- Exception hierarchy: `ChromaException` (base), `ChromaClientException` (4xx), `ChromaServerException` (5xx), `ChromaConnectionException` (network/timeout), `ChromaDeserializationException` (malformed responses)
- Specific 4xx exceptions: `ChromaUnauthorizedException` (401), `ChromaForbiddenException` (403), `ChromaNotFoundException` (404), `ChromaConflictException` (409), `ChromaBadRequestException` (400)
- Default local embedding function using ONNX Runtime — no API key required (all-MiniLM-L6-v2 model)
- Embedding function precedence: explicit runtime EF > `configuration.embedding_function` > schema default EF
- WireMock conformance test suite for all four remote embedding providers (OpenAI, Cohere, HuggingFace, Ollama)
- Embedding provider error normalization: null/empty guard, count-mismatch detection, consistent `ChromaException` wrapping
- Java 8 API compatibility enforcement via `animal-sniffer` bound to `compile` phase
- Public interface compatibility tests (`PublicInterfaceCompatibilityTest`) for API stability guardrails
- Multi-version integration test matrix: Chroma 1.0.0, 1.3.7, 1.5.5 x JDK 8, 11, 17 (5 CI cells)
- `make test-matrix` Makefile target for local reproducible multi-version testing
- `assumeMinVersion()` helper in integration test base class for version-conditional skips
- Fail-fast container startup: bad Docker image tags throw `AssertionError` instead of silently skipping
- Tenant and database lifecycle: `createTenant`, `getTenant`, `createDatabase`, `getDatabase`, `listDatabases`, `deleteDatabase`
- Session context switching: `useTenant(Tenant)`, `useDatabase(Database)`, `currentTenant()`, `currentDatabase()`

### Changed

- Minimum supported Chroma version is now 1.0.0 (was not formally specified)
- Authentication uses typed providers (`BasicAuth.of(...)`, `TokenAuth.of(...)`, `ChromaTokenAuth.of(...)`) instead of manual header manipulation via `setDefaultHeaders()`
- Collection operations use fluent builders terminated by `.execute()` (was positional arguments with nulls)
- Query results returned as `QueryResult` interface (was `Collection.QueryResponse` inner class)
- All exceptions are unchecked (`RuntimeException`-based) for fluent API ergonomics (was checked exceptions)
- Tenant/database create responses are name-authoritative when server returns non-blank name; request name is fallback only
- CI actions upgraded from `actions/checkout@v3` / `setup-java@v3` with `adopt` distribution to `@v4` / `temurin`
- Default CHROMA_VERSION in Makefile updated to `1.5.5`
- DefaultEmbeddingFunction model download uses OkHttp with 300-second timeout and retry instead of URL.openStream()

### Removed

- v1 API classes: `tech.amikos.chromadb.Client`, `tech.amikos.chromadb.Collection`, `tech.amikos.chromadb.Collection.QueryResponse`, `tech.amikos.chromadb.Collection.GetResult`
- Manual auth via `setDefaultHeaders()` with raw `Authorization`/`X-Chroma-Token` headers (use `auth(...)` builder method instead)

### Fixed

- DefaultEmbeddingFunction model download reliability: switched from `URL.openStream()` to OkHttp with 300s timeout, preventing hangs on first-time model download
- Consistent error translation across all API endpoints: all HTTP error responses map to typed `ChromaException` subclasses
- Cloud preflight and identity auth failures now surface as `ChromaUnauthorizedException` (401) or `ChromaForbiddenException` (403) with actionable error messages
- Duplicate generated IDs within the same batch are now rejected client-side before sending the request

[Unreleased]: https://github.com/amikos-tech/chromadb-java-client/compare/0.2.0...HEAD
[0.2.0]: https://github.com/amikos-tech/chromadb-java-client/releases/tag/0.2.0
