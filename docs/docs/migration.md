# Migration from v1

!!! note
    Version 0.2.0 is a major rewrite. The v1 API has been removed.

All v1 classes in `tech.amikos.chromadb` (without the `.v2` suffix) have been replaced by a new builder-based v2 API in `tech.amikos.chromadb.v2`.

## Breaking Changes

### Removed Classes

| Removed Class | v2 Replacement |
|---------------|---------------|
| `tech.amikos.chromadb.Client` | `ChromaClient.builder().build()` (self-hosted) or `ChromaClient.cloud().build()` (Cloud) |
| `tech.amikos.chromadb.Collection` | `tech.amikos.chromadb.v2.Collection` |
| `tech.amikos.chromadb.Collection.QueryResponse` | `tech.amikos.chromadb.v2.QueryResult` |
| `tech.amikos.chromadb.Collection.GetResult` | `tech.amikos.chromadb.v2.GetResult` |

### Changed Packages

All v2 client types are in `tech.amikos.chromadb.v2`. Embedding functions remain in `tech.amikos.chromadb.embeddings.*` (unchanged).

### Changed Authentication

The `client.setDefaultHeaders()` approach with manual header construction has been replaced by typed auth providers:

- `BasicAuth.of(username, password)` — HTTP Basic
- `TokenAuth.of(token)` — Bearer token
- `ChromaTokenAuth.of(token)` — X-Chroma-Token header

### Changed API Surface

- Collection operations use fluent builders terminated by `.execute()` instead of positional arguments.
- All exceptions are unchecked (`RuntimeException`-based) for fluent API ergonomics.
- Auth providers are typed objects rather than raw header maps.

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

=== "v1 (Removed)"

    ```java
    import tech.amikos.chromadb.Client;

    Client client = new Client(System.getenv("CHROMA_URL"));
    ```

=== "v2 (Current)"

    ```java
    import tech.amikos.chromadb.v2.*;

    Client client = ChromaClient.builder()
            .baseUrl(System.getenv("CHROMA_URL"))
            .build();
    ```

### Adding Documents

=== "v1 (Removed)"

    ```java
    import tech.amikos.chromadb.Collection;

    // positional nulls for unused parameters
    collection.add(null, metadatas, documents, ids);
    ```

=== "v2 (Current)"

    ```java
    import tech.amikos.chromadb.v2.*;

    collection.add()
            .documents("doc1", "doc2")
            .metadatas(meta1, meta2)
            .ids("id-1", "id-2")
            .execute();
    ```

### Querying

=== "v1 (Removed)"

    ```java
    import tech.amikos.chromadb.Collection;

    Collection.QueryResponse qr = collection.query(
            Arrays.asList("search text"), 10, null, null, null);
    ```

=== "v2 (Current)"

    ```java
    import tech.amikos.chromadb.v2.*;

    QueryResult result = collection.query()
            .queryTexts("search text")
            .nResults(10)
            .include(Include.DOCUMENTS, Include.DISTANCES)
            .execute();
    ```

### Authentication

=== "v1 (Removed)"

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

=== "v2 (Current)"

    ```java
    import tech.amikos.chromadb.v2.*;

    Client client = ChromaClient.builder()
            .baseUrl(System.getenv("CHROMA_URL"))
            .auth(BasicAuth.of("admin", "password"))
            .build();
    ```

## Next Steps

- [Client Setup](client.md) — connecting, auth, tenant/database configuration
- [Records](records.md) — add, query, get, update, upsert, delete
