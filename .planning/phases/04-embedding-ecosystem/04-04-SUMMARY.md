---
phase: 04-embedding-ecosystem
plan: 04
subsystem: embeddings
tags: [bm25, sparse-vectors, murmur3, snowball, splade, chroma-cloud]

# Dependency graph
requires:
  - phase: 04-embedding-ecosystem/04-01
    provides: SparseEmbeddingFunction interface and SparseVector type
provides:
  - BM25EmbeddingFunction for local sparse vector generation
  - BM25Tokenizer pipeline (lowercase, split, stop words, Snowball stem, Murmur3 hash)
  - Murmur3 x86 32-bit inline hash implementation
  - ChromaCloudSpladeEmbeddingFunction for remote sparse embedding via Chroma Cloud API
affects: [04-embedding-ecosystem/04-05, documentation-site]

# Tech tracking
tech-stack:
  added: [snowball-stemmer 1.3.0.581.1]
  patterns: [SparseEmbeddingFunction provider pattern, inline hash (no Guava)]

key-files:
  created:
    - src/main/java/tech/amikos/chromadb/embeddings/bm25/Murmur3.java
    - src/main/java/tech/amikos/chromadb/embeddings/bm25/BM25StopWords.java
    - src/main/java/tech/amikos/chromadb/embeddings/bm25/BM25Tokenizer.java
    - src/main/java/tech/amikos/chromadb/embeddings/bm25/BM25EmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/chromacloudsplade/ChromaCloudSpladeEmbeddingFunction.java
    - src/main/java/tech/amikos/chromadb/embeddings/chromacloudsplade/CreateSparseEmbeddingRequest.java
    - src/main/java/tech/amikos/chromadb/embeddings/chromacloudsplade/CreateSparseEmbeddingResponse.java
    - src/test/java/tech/amikos/chromadb/embeddings/bm25/TestMurmur3.java
    - src/test/java/tech/amikos/chromadb/embeddings/bm25/TestBM25Tokenizer.java
    - src/test/java/tech/amikos/chromadb/embeddings/bm25/TestBM25EmbeddingFunction.java
    - src/test/java/tech/amikos/chromadb/embeddings/TestChromaCloudSpladeEmbeddingFunction.java
  modified:
    - pom.xml

key-decisions:
  - "englishStemmer class name is lowercase (org.tartarus.snowball.ext.englishStemmer) in snowball-stemmer 1.3.0.581.1"
  - "BM25StopWords contains 179 words (NLTK English stopwords), not 174 as stated in plan -- actual count from Go client list"
  - "ChromaCloudSplade uses Bearer token auth (Authorization: Bearer) for Chroma Cloud API consistency"

patterns-established:
  - "SparseEmbeddingFunction provider: implements interface, returns SparseVector, follows WithParam config pattern"
  - "Inline hash: Murmur3 x86 32-bit without external dependency (no Guava)"

requirements-completed: [EMB-05]

# Metrics
duration: 6min
completed: 2026-04-01
---

# Phase 4 Plan 4: BM25 & Chroma Cloud Splade Sparse Embedding Providers Summary

**BM25 local sparse embeddings with Murmur3/Snowball/stop-words pipeline plus ChromaCloudSplade remote provider, both implementing SparseEmbeddingFunction with 28 passing tests**

## Performance

- **Duration:** 6 min
- **Started:** 2026-04-01T12:51:40Z
- **Completed:** 2026-04-01T12:58:12Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments
- BM25 tokenizer pipeline matching Go client: lowercase, regex split, 179-word NLTK stop word filter, Snowball English stemmer, Murmur3 x86 32-bit hash
- BM25EmbeddingFunction producing sorted SparseVector output with K=1.2, B=0.75, avgDocLen=256 defaults
- ChromaCloudSpladeEmbeddingFunction calling Chroma Cloud API with Bearer token auth
- 28 unit tests covering Murmur3 known vectors, tokenizer edge cases, BM25 scoring, and WireMock Splade integration

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement Murmur3, BM25Tokenizer, BM25StopWords, and BM25EmbeddingFunction** - `05e14ec` (feat)
2. **Task 2: ChromaCloudSplade provider and unit tests for BM25 + Splade** - `747f5b4` (test)

## Files Created/Modified
- `pom.xml` - Added snowball-stemmer 1.3.0.581.1 dependency
- `src/main/java/tech/amikos/chromadb/embeddings/bm25/Murmur3.java` - Murmur3 x86 32-bit inline hash
- `src/main/java/tech/amikos/chromadb/embeddings/bm25/BM25StopWords.java` - 179 NLTK English stop words
- `src/main/java/tech/amikos/chromadb/embeddings/bm25/BM25Tokenizer.java` - Tokenizer pipeline with Snowball stemming
- `src/main/java/tech/amikos/chromadb/embeddings/bm25/BM25EmbeddingFunction.java` - BM25 scoring producing SparseVector
- `src/main/java/tech/amikos/chromadb/embeddings/chromacloudsplade/ChromaCloudSpladeEmbeddingFunction.java` - Remote Chroma Cloud Splade provider
- `src/main/java/tech/amikos/chromadb/embeddings/chromacloudsplade/CreateSparseEmbeddingRequest.java` - Splade API request DTO
- `src/main/java/tech/amikos/chromadb/embeddings/chromacloudsplade/CreateSparseEmbeddingResponse.java` - Splade API response DTO
- `src/test/java/tech/amikos/chromadb/embeddings/bm25/TestMurmur3.java` - 6 hash tests including known Python mmh3 vectors
- `src/test/java/tech/amikos/chromadb/embeddings/bm25/TestBM25Tokenizer.java` - 8 tokenizer tests
- `src/test/java/tech/amikos/chromadb/embeddings/bm25/TestBM25EmbeddingFunction.java` - 8 BM25 scoring tests
- `src/test/java/tech/amikos/chromadb/embeddings/TestChromaCloudSpladeEmbeddingFunction.java` - 6 WireMock tests

## Decisions Made
- englishStemmer class name is lowercase in snowball-stemmer 1.3.0.581.1 (not EnglishStemmer as plan specified)
- BM25StopWords contains 179 words (actual count from plan's listed words), not 174 as stated in plan text
- ChromaCloudSplade uses Bearer token auth for Chroma Cloud API consistency with other providers

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Snowball EnglishStemmer class name is lowercase**
- **Found during:** Task 1 (BM25Tokenizer implementation)
- **Issue:** Plan specified `EnglishStemmer` but actual class in snowball-stemmer jar is `englishStemmer` (lowercase)
- **Fix:** Changed import and usage to `org.tartarus.snowball.ext.englishStemmer`
- **Files modified:** BM25Tokenizer.java
- **Verification:** `mvn compile` succeeds
- **Committed in:** 05e14ec (Task 1 commit)

**2. [Rule 1 - Bug] Stop words count is 179 not 174**
- **Found during:** Task 2 (TestBM25Tokenizer)
- **Issue:** Plan text says "174 words" but the actual word list in the plan contains 179 unique words
- **Fix:** Test asserts 179 instead of 174; BM25StopWords.java uses the complete list from the plan
- **Files modified:** TestBM25Tokenizer.java
- **Verification:** All tokenizer tests pass
- **Committed in:** 747f5b4 (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for correctness. No scope creep.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- BM25 and ChromaCloudSplade providers ready for registry integration (Plan 05)
- All SparseEmbeddingFunction implementations complete for this phase

## Self-Check: PASSED

All 12 files verified present. Both commit hashes (05e14ec, 747f5b4) found in git log.

---
*Phase: 04-embedding-ecosystem*
*Completed: 2026-04-01*
