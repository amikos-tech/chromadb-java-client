# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Build and Test
- **Build project**: `mvn clean compile`
- **Run all tests**: `mvn test`
- **Run specific test class**: `mvn test -Dtest=TestClassName`
- **Run specific test method**: `mvn test -Dtest=TestClassName#testMethodName`
- **Package JAR**: `mvn clean package`
- **Install to local Maven repository**: `mvn clean install`
- **Skip tests during build**: `mvn clean install -DskipTests`

### Testing with Different Chroma Versions
- **Set Chroma version for tests**: `export CHROMA_VERSION=0.5.15 && mvn test`
- Supported versions: 0.4.24, 0.5.0, 0.5.5, 0.5.15

### Environment Variables for Tests
- `OPENAI_API_KEY` - Required for OpenAI embedding tests
- `COHERE_API_KEY` - Required for Cohere embedding tests
- `HF_API_KEY` - Required for HuggingFace embedding tests
- `CHROMA_VERSION` - Specifies ChromaDB version for integration tests

## Architecture Overview

### V2 API (`tech.amikos.chromadb.v2`)
The client is being migrated to a v2 API with an interface-first design:

1. **Client Interface** (`tech.amikos.chromadb.v2.Client`)
   - Top-level interface for all ChromaDB operations (health, tenant/database CRUD, collection lifecycle)
   - Extends `AutoCloseable`
   - Instances created via `ChromaClient.builder()` (self-hosted) or `ChromaClient.cloud()` (Chroma Cloud)

2. **Collection Interface** (`tech.amikos.chromadb.v2.Collection`)
   - Fluent builder pattern for record operations (add, query, get, update, upsert, delete)
   - Type-safe filter DSL via `Where` and `WhereDocument` classes

3. **Exception Hierarchy** (`tech.amikos.chromadb.v2.ChromaException`)
   - Unchecked (RuntimeException-based) for fluent API ergonomics
   - `ChromaClientException` (4xx) with specific subclasses: BadRequest (400), Unauthorized (401), Forbidden (403), NotFound (404), Conflict (409)
   - `ChromaServerException` (5xx)
   - `ChromaConnectionException` for network/timeout errors
   - Factory method: `ChromaExceptions.fromHttpResponse(statusCode, message, errorCode)`

4. **Auth Providers** (`AuthProvider` interface)
   - `BasicAuth` — HTTP Basic authentication
   - `TokenAuth` — Bearer token authentication
   - `ChromaTokenAuth` — X-Chroma-Token header authentication

5. **Value Objects**
   - `Tenant`, `Database` — immutable identifiers with equals/hashCode
   - `DistanceFunction`, `Include` — enums for query configuration
   - `CollectionConfiguration` — immutable HNSW parameters via builder

### Embedding Functions (`tech.amikos.chromadb.embeddings.*`)
- Multiple provider implementations (OpenAI, Cohere, HuggingFace, Ollama, Default)
- Each implements `EmbeddingFunction` interface
- Default embedding uses ONNX Runtime for local inference

### Key Design Patterns

1. **Authentication Handling**
   - Supports multiple auth methods: Basic Auth, Bearer Token, X-Chroma-Token
   - Auth providers implement `AuthProvider` interface and apply headers via `applyAuth()`

2. **Collection Management**
   - Collections are the primary abstraction for vector storage
   - Record operations use fluent builders terminated by `execute()`
   - Metadata and documents are stored alongside embeddings

3. **Sync Operations**
   - Primary operations are synchronous
   - HTTP client uses OkHttp with configurable timeouts

### Testing Strategy

- **Unit Tests**: Test v2 value objects, auth providers, exception hierarchy, and builders
- **Integration Tests**: Use TestContainers with actual ChromaDB Docker images
- **Test Container**: `chromadb/chroma` with configurable versions
- Tests verify compatibility across multiple ChromaDB versions (0.4.24 to 0.5.15)

## Development Notes

### Adding New Embedding Functions
1. Implement `EmbeddingFunction` interface in `tech.amikos.chromadb.embeddings`
2. Add corresponding tests in test directory
3. Update README.md with usage examples

### Modifying V2 API
1. Update or add interfaces/classes in `tech.amikos.chromadb.v2`
2. Add corresponding unit tests
3. Ensure Java 8 compatibility is maintained

### Version Compatibility
- Target Java 8 for maximum compatibility
- ChromaDB versions: `>=0.4.3+ <1.0.0`
- Maintain backward compatibility when possible

### Publishing
- Artifacts published to Maven Central under `io.github.amikos-tech:chromadb-java-client`
- Release process triggered via GitHub Actions on tag push
- Signing required for Maven Central deployment
