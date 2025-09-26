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

### Code Generation
- **Generate API client from OpenAPI spec**: `mvn generate-sources`
- Generated code location: `target/generated-sources/swagger/`
- OpenAPI specs location: `src/main/resources/openapi/`

### Testing with Different Chroma Versions
- **Set Chroma version for tests**: `export CHROMA_VERSION=0.5.15 && mvn test`
- Supported versions: 0.4.24, 0.5.0, 0.5.5, 0.5.15

### Environment Variables for Tests
- `OPENAI_API_KEY` - Required for OpenAI embedding tests
- `COHERE_API_KEY` - Required for Cohere embedding tests
- `HF_API_KEY` - Required for HuggingFace embedding tests
- `CHROMA_VERSION` - Specifies ChromaDB version for integration tests

## Architecture Overview

### Core Client Structure
The client follows a standard Swagger/OpenAPI generated client pattern with custom enhancements:

1. **Generated API Layer** (`target/generated-sources/swagger/`)
   - Auto-generated from OpenAPI specifications
   - Provides low-level API operations
   - Should not be manually edited

2. **Client Wrapper** (`tech.amikos.chromadb.Client`)
   - High-level interface wrapping generated API
   - Manages collections, embeddings, and queries
   - Handles authentication and configuration

3. **Embedding Functions** (`tech.amikos.chromadb.embeddings.*`)
   - Multiple provider implementations (OpenAI, Cohere, HuggingFace, Ollama, Default)
   - Each implements `EmbeddingFunction` interface
   - Default embedding uses ONNX Runtime for local inference

### Key Design Patterns

1. **Authentication Handling**
   - Supports multiple auth methods: Basic Auth, Bearer Token, X-Chroma-Token
   - Configured via `ClientConfig` during client initialization

2. **Collection Management**
   - Collections are the primary abstraction for vector storage
   - Each collection has an associated embedding function
   - Metadata and documents are stored alongside embeddings

3. **Async/Sync Operations**
   - Primary operations are synchronous
   - HTTP client uses OkHttp with configurable timeouts

### Testing Strategy

- **Unit Tests**: Mock HTTP responses using WireMock
- **Integration Tests**: Use TestContainers with actual ChromaDB Docker images
- **Test Container**: `chromadb/chroma` with configurable versions
- Tests verify compatibility across multiple ChromaDB versions (0.4.24 to 0.5.15)

## Development Notes

### Adding New Embedding Functions
1. Implement `EmbeddingFunction` interface in `tech.amikos.chromadb.embeddings`
2. Add corresponding tests in test directory
3. Update README.md with usage examples

### Modifying API Operations
1. Update OpenAPI spec in `src/main/resources/openapi/`
2. Run `mvn generate-sources` to regenerate client code
3. Update wrapper methods in `Client` class if needed

### Version Compatibility
- Target Java 8 for maximum compatibility
- ChromaDB versions: `>=0.4.3+ <1.0.0`
- Maintain backward compatibility when possible

### Publishing
- Artifacts published to Maven Central under `io.github.amikos-tech:chromadb-java-client`
- Release process triggered via GitHub Actions on tag push
- Signing required for Maven Central deployment