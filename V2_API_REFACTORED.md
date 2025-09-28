# Chroma V2 API - Refactored Design

Clean, minimal abstraction with separation of concerns between client management and collection operations.

## Architecture

### Client Interface
Single interface for all tenant/database/collection management operations.

### Implementations
- **`ServerClient`** - Self-hosted Chroma server
- **`CloudClient`** - Chroma Cloud (future)

### Collection as Active Entity
Collection is a smart entity with embedded record operations (add, query, update, delete, upsert).

## Quick Start

### 1. Create a ServerClient

```java
import tech.amikos.chromadb.v2.client.ServerClient;
import tech.amikos.chromadb.v2.auth.AuthProvider;

ServerClient client = ServerClient.builder()
    .baseUrl("http://localhost:8000")
    .auth(AuthProvider.token("your-token"))
    .defaultTenant("default")
    .defaultDatabase("default")
    .build();
```

### 2. Create a Collection

```java
import tech.amikos.chromadb.v2.model.*;

// Using defaults
Collection collection = client.createCollection("my-collection");

// With configuration
Collection collection = client.createCollection("my-collection", config -> config
    .metadata(Map.of("description", "My collection"))
    .configuration(CollectionConfiguration.builder()
        .hnsw(HnswConfiguration.builder()
            .space("l2")
            .efConstruction(200)
            .build())
        .build())
);
```

### 3. Add Records to Collection

```java
// Fluent builder style
collection.add()
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
    .execute();

// Lambda configurator style
collection.add(add -> add
    .ids(List.of("id1", "id2"))
    .embeddings(embeddings)
    .documents(documents)
);
```

### 4. Query Collection

```java
import tech.amikos.chromadb.v2.model.Include;
import tech.amikos.chromadb.v2.model.Where;

// Fluent builder
QueryResponse results = collection.query()
    .queryEmbeddings(List.of(List.of(0.1f, 0.2f, 0.3f)))
    .nResults(10)
    .where(Where.eq("type", "article"))
    .include(Include.DOCUMENTS, Include.DISTANCES, Include.METADATAS)
    .execute();

// Lambda configurator
QueryResponse results = collection.query(query -> query
    .queryEmbeddings(embeddings)
    .nResults(10)
    .where(Where.eq("type", "article"))
);
```

### 5. Get Records from Collection

```java
// Get all records
GetResponse all = collection.get().execute();

// Get with filters
GetResponse filtered = collection.get()
    .where(Where.eq("type", "article"))
    .include(Include.DOCUMENTS, Include.METADATAS)
    .limit(10)
    .offset(0)
    .execute();

// Get by IDs
GetResponse byIds = collection.get()
    .ids(List.of("id1", "id2"))
    .include(Include.DOCUMENTS)
    .execute();
```

### 6. Update Records

```java
collection.update()
    .ids(List.of("id1"))
    .documents(List.of("updated document"))
    .metadatas(List.of(Map.of("type", "updated")))
    .execute();
```

### 7. Upsert Records

```java
collection.upsert()
    .ids(List.of("id1", "id2"))
    .embeddings(embeddings)
    .documents(documents)
    .execute();
```

### 8. Delete Records

```java
// Delete by IDs
collection.delete()
    .ids(List.of("id1", "id2"))
    .execute();

// Delete by metadata filter
collection.delete()
    .where(Where.eq("type", "draft"))
    .execute();

// Delete by document content
collection.delete()
    .whereDocument(WhereDocument.contains("deprecated"))
    .execute();
```

### 9. Count Records

```java
int count = collection.count();
```

## Client Operations

### Collection Management

```java
// Create
Collection col = client.createCollection("default", "default", "my-collection");

// Get or create
Collection col = client.getOrCreateCollection("default", "default", "my-collection");

// Get existing
Collection col = client.getCollection("default", "default", "collection-id");

// List
List<Collection> collections = client.listCollections("default", "default");
List<Collection> limited = client.listCollections("default", "default", 10, 0);

// Count
int count = client.countCollections("default", "default");

// Delete
client.deleteCollection("default", "default", "collection-id");

// Update
client.updateCollection("default", "default", "collection-id", update -> update
    .name("new-name")
    .metadata(Map.of("key", "value"))
);
```

### Using Default Tenant/Database

```java
// Set defaults in builder
ServerClient client = ServerClient.builder()
    .baseUrl("http://localhost:8000")
    .defaultTenant("my-tenant")
    .defaultDatabase("my-database")
    .build();

// Use convenience methods (no tenant/database params)
Collection col = client.createCollection("my-collection");
Collection col = client.getOrCreateCollection("my-collection");
Collection col = client.getCollection("collection-id");
List<Collection> collections = client.listCollections();
int count = client.countCollections();
client.deleteCollection("collection-id");
```

### Database Operations

```java
// Create
Database db = client.createDatabase("my-tenant", "my-database");

// Get
Database db = client.getDatabase("my-tenant", "my-database");

// List
List<Database> databases = client.listDatabases("my-tenant");
List<Database> limited = client.listDatabases("my-tenant", 10, 0);

// Delete
client.deleteDatabase("my-tenant", "my-database");
```

### Tenant Operations

```java
// Create
Tenant tenant = client.createTenant("my-tenant");

// Get
Tenant tenant = client.getTenant("my-tenant");

// Update
client.updateTenant("my-tenant", update -> update
    .resourceName("new-resource-name")
);
```

### Utility Operations

```java
String heartbeat = client.heartbeat();
String version = client.version();
client.reset(); // Dangerous!
```

## CloudClient (Future)

```java
import tech.amikos.chromadb.v2.client.CloudClient;

CloudClient client = CloudClient.builder()
    .apiKey("your-cloud-api-key")
    .region("us-east-1")
    .build();

// Same interface as ServerClient
Collection collection = client.getCollection("tenant", "database", "collection-id");
collection.query()...
```

## Where DSL

```java
// Equality
Where.eq("key", "value")
Where.ne("key", "value")

// Comparison
Where.gt("views", 100)
Where.gte("rating", 4.5)
Where.lt("age", 18)
Where.lte("price", 50.0)

// Membership
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
    .and(Where.gt("views", 1000"))
```

## WhereDocument DSL

```java
// Text contains
WhereDocument.contains("machine learning")
WhereDocument.notContains("deprecated")

// Logical operators
WhereDocument.and(
    WhereDocument.contains("java"),
    WhereDocument.contains("spring")
)

WhereDocument.or(
    WhereDocument.contains("python"),
    WhereDocument.contains("ruby")
)
```

## Include Enum

```java
Include.EMBEDDINGS
Include.DOCUMENTS
Include.METADATAS
Include.DISTANCES
Include.URIS
```

## Authentication

```java
// No auth
AuthProvider.none()

// Bearer token
AuthProvider.token("your-bearer-token")

// Basic auth
AuthProvider.basic("username", "password")

// X-Chroma-Token header
AuthProvider.chromaToken("your-chroma-token")
```

## Benefits

✅ **Clean Separation**: Client for management, Collection for data operations
✅ **Intuitive API**: `collection.query()` feels natural
✅ **Minimal Abstraction**: No unnecessary intermediate layers
✅ **Cloud Ready**: Easy to add CloudClient with cloud-specific features
✅ **Explicit**: Tenant/database always clear (or use defaults)
✅ **Type Safe**: Compile-time checks with builders
✅ **Flexible**: Both fluent and lambda configurator styles

## Comparison with Old Design

### Old (DatabaseClient pattern)
```java
DatabaseClient db = client.defaultDatabase();
Collection collection = db.createCollection("my-collection");
CollectionClient collectionClient = db.collection(collection.getId().toString());
QueryResponse results = collectionClient.query()...
```

### New (Collection as entity)
```java
Collection collection = client.createCollection("my-collection");
QueryResponse results = collection.query()...
```

Much cleaner and more intuitive!