# Migrating to 0.2.0

Version 0.2.0 is a major rewrite. The v1 API classes have been removed and replaced with a new builder-based v2 API.

## Breaking Changes

### Removed Classes

- `tech.amikos.chromadb.Client` — replaced by `ChromaClient.builder().build()` (self-hosted) or `ChromaClient.cloud().build()` (Chroma Cloud)
- `tech.amikos.chromadb.Collection` — replaced by `tech.amikos.chromadb.v2.Collection` interface
- `tech.amikos.chromadb.Collection.QueryResponse` — replaced by `tech.amikos.chromadb.v2.QueryResult`
- `tech.amikos.chromadb.Collection.GetResult` (inner class) — replaced by `tech.amikos.chromadb.v2.GetResult`

### Changed Packages

All v2 client types are in `tech.amikos.chromadb.v2`. Embedding functions remain in `tech.amikos.chromadb.embeddings.*` (unchanged).

### Changed Authentication

Old: `client.setDefaultHeaders()` with manual header construction
New: `ChromaClient.builder().auth(BasicAuth.of(...))` with typed auth providers

### Changed API Surface

- Collection operations now use fluent builders terminated by `.execute()` instead of positional arguments
- All exceptions are unchecked (`RuntimeException`-based) for fluent API ergonomics
- Auth providers are typed objects (`BasicAuth`, `TokenAuth`, `ChromaTokenAuth`) rather than raw header maps

## v1 to v2 Mapping

| v1 Pattern | v2 Equivalent |
|------------|---------------|
| `new Client(url)` | `ChromaClient.builder().baseUrl(url).build()` |
| `client.createCollection(name, meta, getOrCreate, ef)` | `client.getOrCreateCollection(name, CreateCollectionOptions.builder().embeddingFunction(ef).build())` |
| `collection.add(null, metadatas, documents, ids)` | `collection.add().documents(...).metadatas(...).ids(...).execute()` |
| `collection.query(texts, n, null, null, null)` | `collection.query().queryTexts(...).nResults(n).execute()` |
| `Collection.QueryResponse qr` | `QueryResult result` |
| `client.setDefaultHeaders(map)` | `ChromaClient.builder().auth(BasicAuth.of(...)).build()` |
| `client.reset()` | `client.reset()` (unchanged) |
| `client.listCollections()` | `client.listCollections()` (return type changed to `List<Collection>`) |
| `client.deleteCollection(name)` | `client.deleteCollection(name)` (unchanged) |
| `client.heartbeat()` | `client.heartbeat()` (unchanged) |

## Before and After Examples

### Connecting

**v1 (removed):**
```java
import tech.amikos.chromadb.Client;

Client client = new Client(System.getenv("CHROMA_URL"));
```

**v2 (current):**
```java
import tech.amikos.chromadb.v2.*;

Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .build();
```

### Adding Documents

**v1 (removed):**
```java
import tech.amikos.chromadb.Collection;

// positional nulls for unused parameters
collection.add(null, metadatas, documents, ids);
```

**v2 (current):**
```java
import tech.amikos.chromadb.v2.*;

collection.add()
        .documents("doc1", "doc2")
        .metadatas(meta1, meta2)
        .ids("id-1", "id-2")
        .execute();
```

### Querying

**v1 (removed):**
```java
import tech.amikos.chromadb.Collection;

Collection.QueryResponse qr = collection.query(
        Arrays.asList("search text"), 10, null, null, null);
```

**v2 (current):**
```java
import tech.amikos.chromadb.v2.*;

QueryResult result = collection.query()
        .queryTexts("search text")
        .nResults(10)
        .include(Include.DOCUMENTS, Include.DISTANCES)
        .execute();
```

### Authentication

**v1 (removed):**
```java
import tech.amikos.chromadb.Client;
import java.util.Base64;
import java.util.HashMap;

Client client = new Client(System.getenv("CHROMA_URL"));
String encoded = Base64.getEncoder().encodeToString("admin:password".getBytes());
client.setDefaultHeaders(new HashMap<String, String>() {{
    put("Authorization", "Basic " + encoded);
}});
```

**v2 (current):**
```java
import tech.amikos.chromadb.v2.*;

Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .auth(BasicAuth.of("admin", "password"))
        .build();
```

## Next Steps

See the [README](README.md) for complete v2 API usage examples including authentication, cloud connection, schema/CMEK configuration, and embedding functions.
