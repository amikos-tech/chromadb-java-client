---
phase: 04-embedding-ecosystem
plan: 03
subsystem: embeddings
tags: [gemini, bedrock, voyage, google-genai, aws-sdk, okhttp, embedding-function]

# Dependency graph
requires:
  - phase: 03-embeddings-id-extensibility
    provides: EmbeddingFunction interface, WithParam configuration pattern, EFException hierarchy
provides:
  - GeminiEmbeddingFunction via Google GenAI SDK
  - BedrockEmbeddingFunction via AWS SDK BedrockRuntime
  - VoyageEmbeddingFunction via OkHttp REST calls
affects: [04-embedding-ecosystem, documentation-site]

# Tech tracking
tech-stack:
  added: [com.google.genai:google-genai:1.2.0, software.amazon.awssdk:bedrockruntime:2.34.0]
  patterns: [lazy-client-init, optional-maven-deps, wiremock-base-api-injection]

key-files:
  created:
    - src/main/java/tech/amikos/chromadb/embeddings/gemini/GeminiEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/bedrock/BedrockEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/voyage/VoyageEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/voyage/CreateEmbeddingRequest.java
    - src/main/java/tech/amikos/chromadb/embeddings/voyage/CreateEmbeddingResponse.java
    - src/test/java/tech/amikos/chromadb/embeddings/TestGeminiEmbeddingFunction.java
    - src/test/java/tech/amikos/chromadb/embeddings/TestBedrockEmbeddingFunction.java
    - src/test/java/tech/amikos/chromadb/embeddings/TestVoyageEmbeddingFunction.java
  modified:
    - pom.xml

key-decisions:
  - "Jackson version aligned to 2.17.2 via dependencyManagement to resolve conflict between nd4j (2.13.4) and Google GenAI SDK (2.17.2)"
  - "Voyage WireMock tests use WithParam.baseAPI() constructor injection instead of static field reflection for URL override"
  - "Gemini and Bedrock clients use lazy initialization (double-checked locking) to avoid SDK load at construction time"
  - "BedrockEmbeddingFunction.region() is a custom WithParam subclass (inner class) for AWS region configuration"

patterns-established:
  - "Lazy SDK client init: vendor SDK clients initialized on first embed call, not in constructor"
  - "Optional Maven deps: vendor SDK deps marked <optional>true</optional> so users opt-in"
  - "WireMock URL injection: test uses WithParam.baseAPI(wireMockUrl) constructor param instead of static field manipulation"

requirements-completed: [EMB-07]

# Metrics
duration: 8min
completed: 2026-04-01
---

# Phase 04 Plan 03: Dense Embedding Providers Summary

**Three new dense embedding providers (Gemini, Bedrock, Voyage) with optional SDK dependencies, WithParam config, and WireMock test coverage**

## Performance

- **Duration:** 8 min
- **Started:** 2026-04-01T12:39:38Z
- **Completed:** 2026-04-01T12:47:29Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- GeminiEmbeddingFunction using Google GenAI SDK with lazy client init and text-embedding-004 default model
- BedrockEmbeddingFunction using AWS SDK with default credential chain, custom region support, and Titan embed v2 default
- VoyageEmbeddingFunction using OkHttp with document/query input_type differentiation and voyage-3.5 default model
- 16 unit tests: 5 Gemini (construction/config), 5 Bedrock (construction/config/region), 6 Voyage (WireMock: embed, input_type, auth, errors)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Maven dependencies and create Gemini, Bedrock, Voyage provider implementations** - `366aeb0` (feat)
2. **Task 2: Unit tests for Gemini, Bedrock, and Voyage providers** - `1ff4825` (test)

## Files Created/Modified
- `pom.xml` - Added google-genai and bedrockruntime optional deps, Jackson version alignment
- `src/main/java/tech/amikos/chromadb/embeddings/gemini/GeminiEmbeddingFunction.java` - Gemini provider via Google GenAI SDK
- `src/main/java/tech/amikos/chromadb/embeddings/bedrock/BedrockEmbeddingFunction.java` - AWS Bedrock provider via AWS SDK
- `src/main/java/tech/amikos/chromadb/embeddings/voyage/VoyageEmbeddingFunction.java` - Voyage AI provider via OkHttp
- `src/main/java/tech/amikos/chromadb/embeddings/voyage/CreateEmbeddingRequest.java` - Voyage API request DTO
- `src/main/java/tech/amikos/chromadb/embeddings/voyage/CreateEmbeddingResponse.java` - Voyage API response DTO with toEmbeddings()
- `src/test/java/tech/amikos/chromadb/embeddings/TestGeminiEmbeddingFunction.java` - Gemini construction/config tests
- `src/test/java/tech/amikos/chromadb/embeddings/TestBedrockEmbeddingFunction.java` - Bedrock construction/config/region tests
- `src/test/java/tech/amikos/chromadb/embeddings/TestVoyageEmbeddingFunction.java` - Voyage WireMock integration tests

## Decisions Made
- Jackson version aligned to 2.17.2 via dependencyManagement to resolve conflict between nd4j transitive dep (2.13.4) and Google GenAI SDK requirement (2.17.2)
- Voyage WireMock tests use `WithParam.baseAPI(wireMockUrl)` constructor injection instead of static field reflection -- more robust and avoids class-load-time default list initialization issue
- Gemini and Bedrock use double-checked locking lazy init for vendor SDK clients to avoid heavyweight SDK initialization at construction time
- BedrockEmbeddingFunction.region() is a custom WithParam subclass (private inner class) that stores region under "awsRegion" config key

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Jackson version conflict between nd4j and Google GenAI SDK**
- **Found during:** Task 2 (test execution)
- **Issue:** nd4j pulls jackson-core:2.13.4, Google GenAI SDK needs 2.17.2+; `BufferRecycler.releaseToPool()` NoSuchMethodError at runtime
- **Fix:** Added jackson-core and jackson-annotations 2.17.2 to dependencyManagement section
- **Files modified:** pom.xml
- **Verification:** All 16 tests pass, `mvn compile` succeeds
- **Committed in:** 1ff4825 (Task 2 commit)

**2. [Rule 3 - Blocking] Gemini SDK API signature differs from plan assumptions**
- **Found during:** Task 1 (compilation)
- **Issue:** Plan specified `embedContent(model, Content, null)` but actual SDK API is `embedContent(model, String, EmbedContentConfig)`. Response uses `embeddings()` (Optional<List<ContentEmbedding>>) not `embedding()`
- **Fix:** Updated to correct SDK API: pass plain string, extract from `response.embeddings().orElseThrow()` then `contentEmbedding.values().orElseThrow()`
- **Files modified:** GeminiEmbeddingFunction.java
- **Verification:** `mvn compile` succeeds
- **Committed in:** 366aeb0 (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both fixes necessary for compilation and test execution. No scope creep.

## Issues Encountered
None beyond the deviations documented above.

## User Setup Required
None - no external service configuration required. SDK dependencies are optional; users add them to their own projects.

## Known Stubs
None - all providers are fully implemented with real SDK calls.

## Next Phase Readiness
- Three new dense providers ready for use (Gemini, Bedrock, Voyage)
- Provider ecosystem expanded from 5 to 8 providers
- Ready for embedding function registry or additional provider plans

## Self-Check: PASSED

All 9 created files verified present. Both task commits (366aeb0, 1ff4825) verified in git log.

---
*Phase: 04-embedding-ecosystem*
*Completed: 2026-04-01*
