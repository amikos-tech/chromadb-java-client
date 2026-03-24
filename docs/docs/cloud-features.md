# Cloud Features

Chroma Cloud provides managed infrastructure with features not available in self-hosted deployments.

## Connecting to Chroma Cloud

Use `ChromaClient.cloud()` to connect to Chroma Cloud. Authentication requires an API key, tenant, and database name.

```java
--8<-- "CloudExample.java:cloud-client"
```

The tenant and database values can be found in the [Chroma Cloud dashboard](https://cloud.trychroma.com).

## Cloud vs Self-Hosted Feature Parity

| Operation | Self-Hosted | Chroma Cloud | Notes |
|-----------|:-----------:|:------------:|-------|
| `heartbeat()` | Yes | Yes | |
| `version()` | Yes | Yes | |
| `preFlight()` | Yes | Yes | |
| `getIdentity()` | Yes | Yes | |
| `reset()` | Yes | No | Server-side reset, not available on cloud |
| `createTenant()` | Yes | Yes | |
| `getTenant()` | Yes | Yes | |
| `createDatabase()` | Yes | Yes | |
| `getDatabase()` | Yes | Yes | |
| `listDatabases()` | Yes | Yes | |
| `deleteDatabase()` | Yes | Yes | |
| `createCollection()` | Yes | Yes | |
| `getCollection()` | Yes | Yes | |
| `getOrCreateCollection()` | Yes | Yes | |
| `listCollections()` | Yes | Yes | |
| `deleteCollection()` | Yes | Yes | |
| `countCollections()` | Yes | Yes | |
| `collection.add()` | Yes | Yes | |
| `collection.query()` | Yes | Yes | |
| `collection.get()` | Yes | Yes | |
| `collection.update()` | Yes | Yes | |
| `collection.upsert()` | Yes | Yes | |
| `collection.delete()` | Yes | Yes | |
| `collection.count()` | Yes | Yes | |
| `collection.modifyName()` | Yes | Yes | |
| `collection.modifyMetadata()` | Yes | Yes | |
| `collection.modifyConfiguration()` | Yes | Yes | |
| `collection.fork()` | No | Yes | Copy-on-write; 256-fork-edge limit |
| `collection.forkCount()` | No | Yes | |
| `collection.indexingStatus()` | No | Yes | Requires Chroma >= 1.4.1 |

## Fork a Collection

`collection.fork(newName)` creates a copy-on-write fork of an existing collection. The forked collection is independent but shares data with the original until it diverges. Maximum 256 forks per edge.

```java
--8<-- "CloudExample.java:fork"
```

The returned `forked` collection is a fully independent `Collection` reference ready for queries and record operations.

## Fork Count

`collection.forkCount()` returns the number of direct forks originating from this collection.

```java
--8<-- "CloudExample.java:fork-count"
```

## Indexing Status

`collection.indexingStatus()` returns an `IndexingStatus` snapshot with the current indexing progress for the collection.

```java
--8<-- "CloudExample.java:indexing-status"
```

The `IndexingStatus` fields:

| Field | Type | Description |
|-------|------|-------------|
| `getNumIndexedOps()` | `long` | Number of operations that have been indexed |
| `getNumUnindexedOps()` | `long` | Number of operations not yet indexed |
| `getTotalOps()` | `long` | Total number of operations |
| `getOpIndexingProgress()` | `double` | Fraction of operations indexed (0.0–1.0) |

!!! warning "Cloud-only operations"
    `fork()`, `forkCount()`, and `indexingStatus()` are Cloud-only. Self-hosted Chroma returns an error for these operations.

!!! note
    `indexingStatus()` requires Chroma >= 1.4.1.
