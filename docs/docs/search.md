# Search

The Search API provides advanced ranking (KNN, RRF), field projection, groupBy, and read levels.
It uses the `collection.search()` builder and the `Search`, `Knn`, `Rrf`, `GroupBy`, and
`ReadLevel` classes from the `tech.amikos.chromadb.v2` package.

!!! warning "Chroma Cloud Only"
    The Search API is **available in Chroma Cloud only**. Self-hosted Chroma returns
    `501 Not Implemented` for the `/search` endpoint. Future support for single-node
    Chroma is planned but not yet available.

    For basic similarity queries on self-hosted deployments, use `collection.query()` — see
    [Records](records.md).

## KNN Search

K-Nearest Neighbor search finds the records most similar to a query text or embedding vector.

### By Text

The query text is sent to the server, which uses the collection's server-side embedding function:

```java
--8<-- "SearchExample.java:knn-text"
```

`Knn.queryText(text).limit(n)` sets the text and per-search result limit.

### By Embedding

Pass a pre-computed embedding vector to bypass server-side embedding:

```java
--8<-- "SearchExample.java:knn-embedding"
```

### With Filters and Projection

Combine KNN search with metadata filters and field projection:

```java
--8<-- "SearchExample.java:knn-with-filter"
```

`.where(...)` applies a `Where` filter to restrict which records are candidates. `.select(...)`
controls which fields are included in the result. Use `Select.ID`, `Select.DOCUMENT`,
`Select.SCORE`, `Select.METADATA`, `Select.EMBEDDING`, or `Select.key("custom_field")`.

## RRF (Reciprocal Rank Fusion)

RRF combines multiple ranked KNN lists into a single unified ranking using reciprocal rank scores:

```java
--8<-- "SearchExample.java:rrf"
```

`Rrf.builder().rank(knn, weight)` adds a KNN sub-ranking with a fusion weight. The `k` parameter
(default 60) controls the RRF smoothing constant. Higher weight gives a sub-ranking more influence
in the final merged list.

## Group By

Group results by a metadata key, returning a bounded number of results per group:

```java
--8<-- "SearchExample.java:group-by"
```

`GroupBy.builder().key("field").maxK(n).minK(m).build()` partitions results by distinct values
of the metadata field. `maxK` limits results per group; `minK` ensures at least `m` per group
when available.

## Read Levels

Control which data sources are queried during the search:

```java
--8<-- "SearchExample.java:read-level"
```

| Read Level | Description |
|---|---|
| `INDEX_AND_WAL` | Reads from both the persisted index and the write-ahead log. Most up-to-date, slightly slower. |
| `INDEX_ONLY` | Reads from the persisted index only. Faster but may miss recently written records. |

Set `.readLevel(...)` on the `SearchBuilder` (not on the individual `Search` object).

## Batch Search

Submit multiple independent search configurations in a single request:

```java
--8<-- "SearchExample.java:batch-search"
```

Each `Search` in the array produces an independent result group. Access results by
`sr.rows(searchIndex)` where the index corresponds to the position in the `searches(...)` call.

## Working with Results

Iterate results with the row-based API:

```java
--8<-- "SearchExample.java:search-result"
```

`sr.rows(searchIndex)` returns a `ResultGroup<SearchResultRow>`. Each `SearchResultRow`
provides `getId()`, `getDocument()`, `getMetadata()`, `getEmbedding()`, and `getScore()`.
Fields not included in the `Select` projection return `null`.
