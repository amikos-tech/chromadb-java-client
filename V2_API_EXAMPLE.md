# Chroma V2 API - Java Client

This document demonstrates the new fluent API for Chroma V2.

## Features

- **No Swagger Codegen**: Hand-crafted POJOs for better control and clarity
- **Fluent API**: Method chaining for excellent developer experience
- **Type-safe**: Leverage Java's type system for compile-time safety
- **Builder Pattern**: All complex operations use builders
- **Immutable Models**: Thread-safe, immutable data models
- **Where/Include DSL**: Type-safe metadata filtering and field selection

## Quick Start

### 1. Create a Client

```java
import tech.amikos.chromadb.v2.client.ChromaClient;
import tech.amikos.chromadb.v2.auth.AuthProvider;

ChromaClient client = ChromaClient.builder()
    .baseUrl("http://localhost:8000")
    .auth(AuthProvider.token("your-token"))
    .build();
```

### 2. Create a Collection

```java
import tech.amikos.chromadb.v2.model.*;

Collection collection = client
    .defaultDatabase()
    .createCollection("my-collection", config -> config
        .metadata(Map.of("description", "My first collection"))
        .configuration(CollectionConfiguration.builder()
            .hnsw(HnswConfiguration.builder()
                .space("l2")
                .efConstruction(200)
                .build())
            .build())
    );
```

### 3. Add Records

```java
client
    .collection(collection.getId().toString())
    .add(add -> add
        .ids(List.of("id1", "id2", "id3"))
        .embeddings(List.of(
            List.of(0.1f, 0.2f, 0.3f),
            List.of(0.4f, 0.5f, 0.6f),
            List.of(0.7f, 0.8f, 0.9f)
        ))
        .documents(List.of("doc1", "doc2", "doc3"))
        .metadatas(List.of(
            Map.of("type", "article"),
            Map.of("type", "blog"),
            Map.of("type", "article")
        ))
    );
```

### 4. Query with Fluent Builder

```java
import tech.amikos.chromadb.v2.model.Include;
import tech.amikos.chromadb.v2.model.Where;

QueryResponse results = client
    .collection(collection.getId().toString())
    .query()
    .queryEmbeddings(List.of(List.of(0.1f, 0.2f, 0.3f)))
    .nResults(10)
    .where(Where.eq("type", "article"))
    .include(Include.DOCUMENTS, Include.DISTANCES, Include.METADATAS)
    .execute();
```

### 5. Alternative Query Style

```java
QueryResponse results = client
    .collection(collection.getId().toString())
    .query(query -> query
        .queryEmbeddings(List.of(List.of(0.1f, 0.2f, 0.3f)))
        .nResults(10)
        .where(Where.eq("type", "article"))
        .include(Include.DOCUMENTS, Include.DISTANCES)
    );
```

### 6. Get Records

```java
GetResponse records = client
    .collection(collection.getId().toString())
    .getRecords()
    .where(Where.eq("type", "article"))
    .include(Include.DOCUMENTS, Include.METADATAS)
    .limit(10)
    .execute();
```

### 7. Update Records

```java
client
    .collection(collection.getId().toString())
    .update()
    .ids(List.of("id1"))
    .documents(List.of("updated document"))
    .metadatas(List.of(Map.of("type", "updated")))
    .execute();
```

### 8. Delete Records

```java
// Delete by IDs
client
    .collection(collection.getId().toString())
    .deleteRecords()
    .ids(List.of("id1", "id2"))
    .execute();

// Delete by metadata filter
client
    .collection(collection.getId().toString())
    .deleteRecords()
    .where(Where.eq("type", "article"))
    .execute();
```

## Where DSL

The `Where` class provides type-safe metadata filtering:

```java
// Simple equality
Where.eq("type", "article")

// Not equal
Where.ne("status", "draft")

// Greater than / less than
Where.gt("views", 100)
Where.gte("rating", 4.5)
Where.lt("age", 18)
Where.lte("price", 50.0)

// In / Not in
Where.in("category", List.of("tech", "science"))
Where.nin("status", List.of("archived", "deleted"))

// Logical operators
Where.and(
    Where.eq("type", "article"),
    Where.gt("views", 1000)
)

Where.or(
    Where.eq("category", "tech"),
    Where.eq("category", "science")
)

// Chaining
Where.eq("type", "article")
    .and(Where.gt("views", 1000))
```

## WhereDocument DSL

Filter by document content:

```java
// Contains text
WhereDocument.contains("machine learning")

// Does not contain
WhereDocument.notContains("deprecated")

// Logical operators
WhereDocument.and(
    WhereDocument.contains("java"),
    WhereDocument.contains("spring")
)
```

## Include Fields

Control which fields to return:

```java
Include.EMBEDDINGS
Include.DOCUMENTS
Include.METADATAS
Include.DISTANCES
Include.URIS
```

## Authentication

```java
// No authentication
AuthProvider.none()

// Bearer token
AuthProvider.token("your-bearer-token")

// Basic auth
AuthProvider.basic("username", "password")

// X-Chroma-Token
AuthProvider.chromaToken("your-chroma-token")
```

## Navigation

```java
// Use default tenant/database
client.collection("collection-id")

// Explicit tenant
client.tenant("my-tenant").database("my-database").collection("collection-id")

// Just database (uses default tenant)
client.database("my-database").collection("collection-id")
```

## Database Operations

```java
// Create collection
Collection col = client.defaultDatabase()
    .createCollection("my-collection");

// Get or create
Collection col = client.defaultDatabase()
    .getOrCreateCollection("my-collection");

// List collections
List<Collection> collections = client.defaultDatabase()
    .listCollections();

// Count collections
int count = client.defaultDatabase()
    .countCollections();

// Get collection
Collection col = client.defaultDatabase()
    .getCollection("collection-id");

// Delete collection
client.defaultDatabase()
    .deleteCollection("collection-id");
```

## Collection Operations

```java
// Get collection metadata
Collection col = client.collection("collection-id").get();

// Count records
int count = client.collection("collection-id").count();

// Delete collection
client.collection("collection-id").delete();
```

## Comparison with V1 API

### V1 API
```java
Client client = new Client("http://localhost:8000");
Collection collection = client.createCollection("test", null, true, ef);
Collection.QueryResponse qr = collection.query(
    Arrays.asList("Who is the spy"),
    10, null, null, null
);
```

### V2 API
```java
ChromaClient client = ChromaClient.builder()
    .baseUrl("http://localhost:8000")
    .build();

Collection collection = client.defaultDatabase()
    .getOrCreateCollection("test");

QueryResponse qr = client.collection(collection.getId().toString())
    .query()
    .queryEmbeddings(embeddings)
    .nResults(10)
    .execute();
```

## Benefits of V2 API

1. **Type Safety**: Compile-time checks for parameters
2. **Discoverability**: IDE autocomplete guides you through available options
3. **Readability**: Fluent API reads like English
4. **Flexibility**: Multiple ways to accomplish the same task
5. **No Magic**: Clear, straightforward code without reflection tricks
6. **Immutability**: Thread-safe models
7. **Builder Pattern**: Optional parameters are easy to handle

## Architecture

```
tech.amikos.chromadb.v2/
├── client/           # ChromaClient, DatabaseClient, CollectionClient
├── model/            # POJOs for requests, responses, and entities
├── http/             # HTTP client abstraction
├── auth/             # Authentication providers
└── exception/        # Exception hierarchy
```

## Error Handling

```java
import tech.amikos.chromadb.v2.exception.*;

try {
    client.collection("non-existent").get();
} catch (ChromaNotFoundException e) {
    System.err.println("Collection not found: " + e.getMessage());
} catch (ChromaUnauthorizedException e) {
    System.err.println("Unauthorized: " + e.getMessage());
} catch (ChromaBadRequestException e) {
    System.err.println("Bad request: " + e.getMessage());
} catch (ChromaServerException e) {
    System.err.println("Server error: " + e.getMessage());
} catch (ChromaV2Exception e) {
    System.err.println("Error: " + e.getMessage());
}
```