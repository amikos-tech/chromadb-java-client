# Records

A record in ChromaDB consists of four components: an **ID** (required), a **document** (text
string), **metadata** (key-value map), and an **embedding** (float vector). At minimum, an ID plus
one of the other components must be provided.

!!! tip
    All record operations use fluent builders terminated by `.execute()`.

## Adding Records

### With Documents and Metadata

```java
--8<-- "RecordsExample.java:add-docs"
```

When documents are provided and the collection has an embedding function, the client embeds the
documents automatically before sending to the server.

### With Pre-Computed Embeddings

```java
--8<-- "RecordsExample.java:add-embeddings"
```

Pass raw embedding vectors directly to skip client-side embedding. Each `float[]` corresponds to
one record.

## Querying

The `query()` method works with both self-hosted and Chroma Cloud deployments. For advanced
search features (KNN ranking, RRF fusion, GroupBy, field projection), see the
[Search API](search.md) (requires Chroma >= 1.5).

### Query by Text

```java
--8<-- "RecordsExample.java:query-text"
```

`queryTexts` requires an embedding function on the collection to convert the text to a vector
before searching. Use `include(...)` to control which fields are returned.

### Query with Filter

```java
--8<-- "RecordsExample.java:query-filter"
```

Combine text queries with metadata filters using `where(Where.eq(...))`. See
[Filtering](filtering.md) for the full filter DSL.

### Query by Embedding

```java
--8<-- "RecordsExample.java:query-embedding"
```

Pass a raw embedding vector to query without an embedding function.

## Getting Records

Retrieve records by ID or filter, optionally controlling which fields are returned:

```java
--8<-- "RecordsExample.java:get"
```

Omit `.ids(...)` to retrieve all records (subject to default server limits).

## Updating Records

Update documents, embeddings, or metadata for existing records by ID:

```java
--8<-- "RecordsExample.java:update"
```

Only fields set on the builder are updated. Unset fields remain unchanged on the server.

## Upserting Records

Insert or update records — creates new records if the ID does not exist, updates if it does:

```java
--8<-- "RecordsExample.java:upsert"
```

## Deleting Records

Delete records by ID:

```java
--8<-- "RecordsExample.java:delete"
```

You can also delete by filter using `.where(...)` or `.whereDocument(...)` instead of `.ids(...)`.

## Counting Records

```java
--8<-- "RecordsExample.java:count"
```

Returns the total number of records in the collection.

## Row-Based Result Access

Query and get results support row-based iteration via `.rows(queryIndex)`:

```java
--8<-- "RecordsExample.java:row-access"
```

`rows(queryIndex)` returns a `List<ResultRow>` for the given query input. Each `ResultRow`
provides `getId()`, `getDocument()`, `getMetadata()`, `getEmbedding()`. Cast to
`QueryResultRow` to access the distance score.

!!! tip
    `rows(0)` returns results for the first query text/embedding. Use `rows(1)` for the second
    input when multiple query texts are provided.

!!! note
    See [Error Handling](error-handling.md) for the unchecked exception hierarchy and common
    recovery patterns for record operations.
