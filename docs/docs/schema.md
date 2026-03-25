# Schema & CMEK

Schema defines the vector index configuration, embedding function spec, and metadata types for a collection. CMEK provides customer-managed encryption keys.

!!! warning "Chroma Cloud Only"
    Schema and CMEK are **available in Chroma Cloud only**. Self-hosted Chroma does not
    support the Schema API or CMEK encryption. For self-hosted HNSW tuning, use
    `CollectionConfiguration` — see [HNSW Configuration](#hnsw-configuration) below.

## Basic Schema

A schema is attached to a collection at creation time. The most common use case is setting the distance function for the embedding vector index.

```java
--8<-- "SchemaExample.java:basic-schema"
```

`Schema.EMBEDDING_KEY` (`"#embedding"`) is the reserved key for the primary embedding vector index. `DistanceFunction` options are `COSINE`, `L2`, and `IP`.

## Schema with Embedding Function

An `EmbeddingFunctionSpec` descriptor tells the server which embedding function to auto-wire for query-by-text operations.

```java
--8<-- "SchemaExample.java:with-ef-spec"
```

`EmbeddingFunctionSpec` fields:

| Field | Description |
|-------|-------------|
| `type` | Descriptor type — use `"known"` for registered server-side providers |
| `name` | Provider name (e.g. `"openai"`, `"cohere"`) |
| `config` | Provider configuration map (e.g. API key environment variable name) |

## CMEK (Customer-Managed Encryption Keys)

CMEK encrypts collection data at rest using a key you control. Currently supported: GCP Cloud KMS.

```java
--8<-- "SchemaExample.java:cmek"
```

The GCP KMS resource name must follow the format:
```
projects/{project}/locations/{location}/keyRings/{keyRing}/cryptoKeys/{key}
```

`Cmek.gcpKms(resourceName)` validates the format at construction time and throws `IllegalArgumentException` for malformed resource names.

## Creating a Collection with Schema

Pass the schema via `CreateCollectionOptions.builder().schema(schema)`.

```java
--8<-- "SchemaExample.java:create-with-schema"
```

## HNSW Configuration

For self-hosted Chroma, use `CollectionConfiguration` to tune the HNSW index parameters directly:

```java
--8<-- "SchemaExample.java:hnsw-config"
```

HNSW parameters:

| Parameter | Method | Description |
|-----------|--------|-------------|
| Distance function | `space(DistanceFunction)` | `L2`, `COSINE`, or `IP` |
| M | `hnswM(int)` | Number of bi-directional links per element (default: 16) |
| efConstruction | `hnswConstructionEf(int)` | Build-time candidate list size; higher = better recall (default: 100) |
| efSearch | `hnswSearchEf(int)` | Query-time candidate list size; higher = better recall (default: 10) |

`CollectionConfiguration` and `Schema` cannot be combined in the same `CreateCollectionOptions` call — use one or the other.

!!! tip
    Use `CollectionConfiguration` for self-hosted HNSW tuning. Use `Schema` (with optional CMEK)
    for Chroma Cloud collections that need server-side embedding function specs, metadata types,
    or encryption at rest.
