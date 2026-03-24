# ID Generators

Generate IDs client-side for `add()` and `upsert()` operations using `.idGenerator(...)` on the builder. This eliminates the need to manually manage IDs when inserting records.

## Comparison

| Generator | Deterministic | Requires | Pattern |
|-----------|:-------------:|----------|---------|
| `UuidIdGenerator` | No | Nothing | Random UUID v4 |
| `UlidIdGenerator` | No | Nothing | Time-ordered ULID |
| `Sha256IdGenerator` | Yes | Document or metadata | Content hash (SHA-256 hex) |

## UUID

`UuidIdGenerator.INSTANCE` generates a random UUID v4 for each record. Works with any data field — embeddings, documents, or metadata.

```java
--8<-- "IdGeneratorsExample.java:uuid"
```

## ULID

`UlidIdGenerator.INSTANCE` generates a Universally Unique Lexicographically Sortable Identifier (ULID). Each ULID encodes a 48-bit millisecond timestamp followed by 80 bits of cryptographic randomness. ULIDs are lexicographically sortable by creation time.

```java
--8<-- "IdGeneratorsExample.java:ulid"
```

## SHA-256 (Content-Based)

`Sha256IdGenerator.INSTANCE` generates a deterministic 64-character lowercase hex ID by hashing the record's document content (or metadata when the document is null). The same document always produces the same ID, enabling content-addressable deduplication.

```java
--8<-- "IdGeneratorsExample.java:sha256"
```

When the document is non-null, only the document is hashed and metadata is ignored. When the document is null and metadata is non-null, the metadata is serialized to a deterministic sorted string and hashed.

## Rules

- `ids(...)` and `idGenerator(...)` are mutually exclusive — setting both throws `IllegalArgumentException` at `execute()` time.
- `idGenerator(...)` requires at least one non-empty data field (`documents`, `embeddings`, `metadatas`, or `uris`) to infer record count.
- `Sha256IdGenerator` requires a non-null document or non-null metadata. Throws `IllegalArgumentException` if both are null.
- Duplicate generated IDs within the same batch are rejected client-side before the request is sent.
