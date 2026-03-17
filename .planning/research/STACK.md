# Stack Research

**Domain:** Java ChromaDB client library (SDK)
**Researched:** 2026-03-17
**Confidence:** HIGH

## Recommended Stack

### Core Technologies

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Java | 8 (baseline) | Runtime compatibility target | Preserves compatibility with existing consumer environments and current project constraints |
| Maven | 3.x | Build, packaging, dependency management | Standard JVM packaging flow and Maven Central publication alignment |
| OkHttp | 4.12.0 | HTTP transport layer | Mature client with interceptors, timeout controls, and stable JVM usage patterns |
| Gson | 2.10.1 | JSON serialization/deserialization | Already integrated and stable with current DTO model shape |

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| JUnit | 4.13.2 | Unit test framework | Core contract and regression tests |
| Testcontainers | 1.21.4 (BOM) | Real Chroma integration tests | Endpoint and behavior verification against containerized Chroma |
| WireMock | 2.35.2 | HTTP stubbing for edge/error tests | Error mapping, timeout behavior, malformed payload tests |
| ONNX Runtime | 1.18.0 | Local default embeddings runtime | For `DefaultEmbeddingFunction` without external API dependency |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Makefile | Common workflow aliases | Keeps build/test/package commands consistent |
| JaCoCo | Coverage reporting | Maintain minimum confidence for refactors |
| PIT | Mutation testing | Validate test strength for critical API paths |
| Maven GPG + Source/Javadoc plugins | Maven Central release readiness | Required for signed releases and proper artifact metadata |

## Installation

```bash
# Build and test
mvn clean test

# Package and install locally
mvn clean install
```

```xml
<dependency>
  <groupId>io.github.amikos-tech</groupId>
  <artifactId>chromadb-java-client</artifactId>
  <version>0.2.0-SNAPSHOT</version>
</dependency>
```

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| OkHttp | Java 11+ HttpClient | If Java 8 support is dropped and standard library-only stack is desired |
| Gson | Jackson | If advanced polymorphic mapping or strict schema tooling becomes a priority |
| JUnit 4 | JUnit 5 | If test infrastructure is modernized and Java baseline constraints are revisited |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| Raw `HttpURLConnection` code paths | Harder retries/timeouts and poorer ergonomics | OkHttp client abstraction |
| Ad-hoc JSON mapping per endpoint | Inconsistent behavior and drift risk | Centralized DTOs with Gson |
| Java-version-specific APIs beyond baseline | Breaks consumer compatibility unexpectedly | Java 8-safe API surface for this milestone |

## Stack Patterns by Variant

**If targeting self-hosted Chroma only:**
- Use `ChromaClient.builder()` with direct base URL and explicit timeout configuration
- Because operational behavior is easier to reason about in controlled infrastructure

**If targeting Chroma Cloud + tenant isolation:**
- Use token-based auth providers and preflight identity checks in bootstrap paths
- Because cloud auth and multi-tenant context are primary failure points if left implicit

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| `okhttp:4.12.0` | Java 8 baseline in this project | Validate shaded/transitive interactions in release builds |
| `gson:2.10.1` | Existing DTO model classes | Ensure unknown-field tolerance stays stable |
| `testcontainers:1.21.4` | Chroma test image matrix | Keep `CHROMA_VERSION` matrix tests in CI |

## Sources

- `pom.xml` — dependency versions and build plugins
- `README.md` — supported features, usage, compatibility notes
- `CLAUDE.md` — architecture and testing strategy context
- [Chroma Usage Guide](https://docs.trychroma.com/) — API/server behavior reference

---
*Stack research for: Java ChromaDB client library*
*Researched: 2026-03-17*
