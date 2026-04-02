---
status: complete
phase: 04-embedding-ecosystem
source: [04-01-SUMMARY.md, 04-02-SUMMARY.md, 04-03-SUMMARY.md, 04-04-SUMMARY.md, 04-05-SUMMARY.md]
started: 2026-04-01T17:15:00Z
updated: 2026-04-01T17:20:00Z
---

## Current Test

[testing complete]

## Tests

### 1. SparseEmbeddingFunction produces SparseVector
expected: BM25EmbeddingFunction.embedQuery("the quick brown fox") returns SparseVector with sorted int[] indices and float[] values, both non-empty.
result: pass

### 2. ContentEmbeddingFunction adapter round-trip
expected: ContentEmbeddingFunction.fromTextOnly(existingDenseEF) wraps any existing EmbeddingFunction. Calling embedContent(Content.text("hello")) delegates to the wrapped EF and returns an Embedding.
result: pass

### 3. Content value types build correctly
expected: Content.text("hello") creates single-part text content. Content.builder().part(Part.image(BinarySource.fromUrl("http://img"))).intent(Intent.RETRIEVAL_DOCUMENT).build() creates multimodal content. Parts list is unmodifiable.
result: pass

### 4. Reranking with Cohere/Jina providers
expected: CohereRerankingFunction(WithParam.apiKey("key")).rerank("query", docs) POSTs to Cohere v2/rerank endpoint. JinaRerankingFunction similarly POSTs to Jina v1/rerank. Both return List<RerankResult> sorted by descending relevance score.
result: pass

### 5. Gemini/Bedrock/Voyage provider construction
expected: GeminiEmbeddingFunction(WithParam.apiKey("key")) constructs without error using lazy client init. BedrockEmbeddingFunction() constructs using AWS default credentials. VoyageEmbeddingFunction(WithParam.apiKey("key")) constructs with OkHttp client. All three are optional Maven deps.
result: pass

### 6. BM25 tokenizer pipeline matches Go client
expected: BM25Tokenizer.tokenize("The quick brown fox") filters stop word "the", lowercases, stems with Snowball English stemmer. Murmur3.hash32("hello".getBytes(UTF_8), 0) produces consistent 32-bit hash matching Python mmh3 output.
result: pass

### 7. EmbeddingFunctionRegistry resolves built-in providers
expected: EmbeddingFunctionRegistry.getDefault().resolveDense(spec("openai")) returns OpenAIEmbeddingFunction. resolveSparse(spec("bm25")) returns BM25EmbeddingFunction. resolveContent(spec("openai")) returns ContentEmbeddingFunction via dense+adapter fallback.
result: pass

### 8. EmbeddingFunctionRegistry custom registration
expected: new EmbeddingFunctionRegistry().registerDense("custom", factory) then resolveDense(spec("custom")) returns the factory result. Thread-safe: concurrent register/resolve calls don't corrupt state.
result: pass

### 9. Full test suite passes with no regressions
expected: `mvn test` passes all ~1209 tests with 0 failures and 0 errors (excluding flaky WireMock timeouts).
result: pass

## Summary

total: 9
passed: 9
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none]
