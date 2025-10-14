# ChromaDB V2 API Documentation

## Overview

The V2 API is an experimental implementation of the ChromaDB v2 client for Java, designed with principles of radical simplicity based on successful Java libraries like OkHttp, Retrofit, and Jedis.

**⚠️ Important:** The v2 API does not yet exist in ChromaDB. This implementation is based on anticipated v2 API design and is provided for experimental/preview purposes only.

## Design Principles

### Radical Simplicity
- **Dual API Approach**: Convenience methods for common cases (80%), builders for complex operations (20%)
- **Chroma-Aligned**: API mirrors official Python/TypeScript SDKs for familiarity
- **Flat Package Structure**: All public API classes in `tech.amikos.chromadb.v2` package (no sub-packages)
- **Simple Things Simple**: Common operations in 1-2 lines, no builders required
- **Minimal Public API Surface**: ~20-25 classes total (following OkHttp's model)
- **Concrete Over Abstract**: Prefer concrete classes over interfaces where possible

## Architecture

```
Client (interface)
  ├── BaseClient (abstract)
  │     ├── ServerClient (self-hosted)
  │     └── CloudClient (cloud - future)
  │
  └── Collection (smart entity with operations)
        ├── query()
        ├── get()
        ├── add()
        ├── update()
        ├── upsert()
        ├── delete()
        └── count()
```

### Core Classes (~20 total)
- `ServerClient` / `CloudClient` - Client implementations
- `Collection` - Concrete collection class (not interface)
- `Metadata` - Strongly-typed metadata with builder
- Query builders: `QueryBuilder`, `AddBuilder`, etc.
- Model classes: `Where`, `WhereDocument`, `Include`
- Auth: `AuthProvider` interface with implementations
- Exceptions: Strongly-typed exception hierarchy

## Quick Start

### 1. Create a Client

```java
import tech.amikos.chromadb.v2.ChromaClient;
import tech.amikos.chromadb.v2.AuthProvider;

ChromaClient client = ChromaClient.builder()
    .serverUrl("http://localhost:8000")
    .auth(AuthProvider.none())
    .tenant("default_tenant")
    .database("default_database")
    .build();
```

### 2. Create a Collection

```java
// Simple creation
Collection collection = client.createCollection("my-collection");

// With metadata
Collection collection = client.createCollection("my-collection",
    Map.of("description", "My collection"));
```

## Simple API (Convenience Methods)

For most use cases, use the simple, Chroma-aligned convenience methods:

### 3. Add Records

```java
// Simple add - mirrors Python/TypeScript Chroma API
collection.add(
    List.of("id1", "id2", "id3"),
    List.of(
        List.of(0.1f, 0.2f, 0.3f),
        List.of(0.4f, 0.5f, 0.6f),
        List.of(0.7f, 0.8f, 0.9f)
    ),
    List.of("Document 1", "Document 2", "Document 3"),
    List.of(
        Map.of("author", "John"),
        Map.of("author", "Jane"),
        Map.of("author", "Bob")
    )
);
```

### 4. Query Collection

```java
// Simple query by embeddings
QueryResponse results = collection.query(
    List.of(List.of(0.1f, 0.2f, 0.3f)),
    10  // number of results
);

// Query with filtering
results = collection.query(
    List.of(List.of(0.1f, 0.2f, 0.3f)),
    10,
    Where.eq("author", "John")
);

// Query by text (auto-embedded)
results = collection.queryByText(
    List.of("quantum computing"),
    5
);
```

### 5. Get Records

```java
// Simple get by IDs
GetResponse records = collection.get(List.of("id1", "id2"));

// Get with includes
records = collection.get(
    List.of("id1", "id2"),
    Include.DOCUMENTS, Include.METADATAS
);
```

### 6. Update/Upsert Records

```java
// Simple upsert
collection.upsert(
    List.of("id4"),
    List.of(List.of(0.2f, 0.3f, 0.4f)),
    List.of("New document")
);
```

### 7. Delete Records

```java
// Delete by IDs
collection.delete(List.of("id1", "id2"));

// Delete by filter
collection.delete(Where.eq("status", "archived"));
```

## Advanced API (Builder Pattern)

For complex operations with multiple options, use the builder pattern:

### Complex Query

```java
QueryResponse results = collection.query()
    .queryEmbeddings(List.of(List.of(0.1f, 0.2f, 0.3f)))
    .nResults(10)
    .where(Where.and(
        Where.eq("status", "published"),
        Where.gte("score", 8.0)
    ))
    .whereDocument(WhereDocument.contains("technology"))
    .include(Include.EMBEDDINGS, Include.METADATAS, Include.DISTANCES)
    .execute();
```

### Complex Get with Pagination

```java
GetResponse records = collection.get()
    .where(Where.eq("category", "tech"))
    .limit(100)
    .offset(0)
    .include(Include.DOCUMENTS, Include.METADATAS)
    .execute();
```

### Complex Add

```java
collection.add()
    .ids(List.of("id1", "id2"))
    .embeddings(embeddings)
    .documents(documents)
    .metadatas(metadatas)
    .uris(uris)
    .execute();
```

## Advanced Features

### Authentication

```java
// Basic authentication
ServerClient client = ServerClient.builder()
    .baseUrl("http://localhost:8000")
    .auth(AuthProvider.basic("username", "password"))
    .build();

// Bearer token
client = ServerClient.builder()
    .baseUrl("http://localhost:8000")
    .auth(AuthProvider.bearerToken("your-api-token"))
    .build();

// X-Chroma-Token header
client = ServerClient.builder()
    .baseUrl("http://localhost:8000")
    .auth(AuthProvider.chromaToken("chroma-token"))
    .build();
```

### Embedding Functions

```java
// Default embedding (uses all-MiniLM-L6-v2)
EmbeddingFunction defaultEF = EmbeddingFunction.getDefault();

// OpenAI embeddings
EmbeddingFunction openAI = EmbeddingFunction.openAI("your-api-key");

// Custom embedding function
EmbeddingFunction custom = new EmbeddingFunction() {
    @Override
    public List<List<Float>> embed(List<String> texts) {
        // Your embedding logic
    }
};

// Use with collection
Collection collection = client.createCollection(builder -> builder
    .name("documents")
    .embeddingFunction(openAI)
);
```

### Metadata Filtering (Where DSL)

```java
// Complex filter conditions
Where filter = Where.builder()
    .and(
        Where.eq("status", "published"),
        Where.gte("score", 8.0),
        Where.or(
            Where.eq("category", "tech"),
            Where.eq("category", "science")
        )
    )
    .build();

// Use in queries
QueryResponse results = collection.query(builder -> builder
    .queryTexts(Arrays.asList("search text"))
    .where(filter)
    .nResults(10)
);
```

### Document Filtering

```java
// Filter by document content
WhereDocument docFilter = WhereDocument.contains("machine learning");

QueryResponse results = collection.query(builder -> builder
    .queryTexts(Arrays.asList("AI research"))
    .whereDocument(docFilter)
    .nResults(5)
);
```

## Implementation Status

### What's Implemented ✅
- Basic client structure (`ServerClient`, `CloudClient`)
- Authentication providers (Basic, Token, ChromaToken)
- Model classes for v2 operations
- Collection operations interface
- Query builder pattern
- Fluent API for all operations
- Type-safe metadata and filtering

### Known Issues ⚠️
1. **API Endpoints:** Currently modified to use `/api/v1` endpoints as a temporary workaround
2. **Tenant/Database Support:** v2 expects multi-tenancy which v1 doesn't fully support
3. **Response Models:** Field names and structure differ between v1 and v2
4. **Embedding Functions:** Integration needs refinement for v2 API

### Coming Soon 🚀
- CloudClient implementation
- Advanced query capabilities
- Batch operations optimization
- Streaming results
- Async/reactive operations

## API Design: Dual Approach

The V2 API offers **two complementary approaches**:

### 1. Convenience Methods (Simple API)
- **For**: 80% of use cases
- **Style**: Direct method calls with parameters
- **Benefit**: Minimal boilerplate, Chroma-aligned
- **Example**: `collection.add(ids, embeddings, documents)`

### 2. Builder Pattern (Advanced API)
- **For**: 20% of complex use cases
- **Style**: Fluent builders with `.execute()`
- **Benefit**: Maximum flexibility, all options available
- **Example**: `collection.query().queryEmbeddings(...).where(...).execute()`

### When to Use Which?

| Use Case | Recommended Approach | Example |
|----------|---------------------|---------|
| Simple add with all data | Convenience | `collection.add(ids, embeddings, documents, metadatas)` |
| Add with URIs or complex options | Builder | `collection.add().ids(...).uris(...).execute()` |
| Basic query | Convenience | `collection.query(embeddings, 10)` |
| Query with whereDocument or complex filters | Builder | `collection.query().queryEmbeddings(...).whereDocument(...).execute()` |
| Get by IDs | Convenience | `collection.get(List.of("id1", "id2"))` |
| Get with pagination | Builder | `collection.get().limit(100).offset(0).execute()` |
| Delete by IDs | Convenience | `collection.delete(ids)` |
| Delete by complex filter | Builder | `collection.delete().where(...).whereDocument(...).execute()` |

### Design Philosophy

> **"Simple things should be simple, complex things should be possible."**

The dual API approach ensures:
- New users can get started quickly with minimal code
- Power users have full control when needed
- API feels familiar to Chroma users from Python/TypeScript
- Java best practices (type safety, clarity) are maintained

## Migration from V1

The V2 API is designed to coexist with V1. Key differences:

| V1 | V2 |
|----|-----|
| `Client` class | `ChromaClient` |
| Swagger-generated models | Hand-crafted POJOs |
| Builder-only patterns | Dual approach (convenience + builders) |
| Multiple ways to configure | Flat, simple API surface |
| Nested packages | Flat package structure |

## Testing

The V2 API includes comprehensive test coverage:

```bash
# Run all V2 tests
mvn test -Dtest="tech.amikos.chromadb.v2.**"

# Run with specific ChromaDB version
export CHROMA_VERSION=1.1.0 && mvn test

# Run stress tests
mvn test -Dtest=V2StressTest
```

## Support

This is an experimental API. For production use, please use the stable V1 API.

For issues or questions:
- GitHub Issues: [chromadb-java-client/issues](https://github.com/amikos-tech/chromadb-java-client/issues)
- Documentation: This file
- Examples: See test files in `src/test/java/tech/amikos/chromadb/v2/`