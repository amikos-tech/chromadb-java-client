# Client Setup

The `Client` interface is the entry point for all ChromaDB operations. Create a client using
`ChromaClient.builder()` for self-hosted deployments or `ChromaClient.cloud()` for Chroma Cloud.

## Self-Hosted

Connect to a self-hosted ChromaDB instance by providing the base URL:

```java
--8<-- "ClientExample.java:self-hosted"
```

Set the `CHROMA_URL` environment variable (e.g., `http://localhost:8000`) before running your
application.

## Chroma Cloud

Connect to Chroma Cloud using your API key, tenant, and database:

```java
--8<-- "ClientExample.java:cloud"
```

Obtain `CHROMA_API_KEY`, `CHROMA_TENANT`, and `CHROMA_DATABASE` from the
[Chroma Cloud dashboard](https://www.trychroma.com).

## Collection Lifecycle

Collections are the primary storage abstraction. The client provides full CRUD for collections:

```java
--8<-- "ClientExample.java:lifecycle"
```

Key behaviors:

- `createCollection` throws `ChromaConflictException` if the collection already exists.
- `getOrCreateCollection` is idempotent and safe to call on startup.
- `getCollection` throws `ChromaNotFoundException` if the collection does not exist.
- `deleteCollection` throws `ChromaNotFoundException` if the collection does not exist.

## With Embedding Function

Attach a client-side embedding function when the collection needs to embed query texts locally:

```java
--8<-- "ClientExample.java:with-ef"
```

The embedding function is stored on the client side and used for text-query operations.
It is not sent to the server.

## Health Check

Verify that the server is reachable and retrieve its version:

```java
--8<-- "ClientExample.java:health"
```

`heartbeat()` returns a nanosecond timestamp string. `version()` returns the server version string.

!!! note
    See [Transport Options](transport.md) for SSL, custom timeouts, and OkHttpClient configuration.

!!! note
    See [Authentication](auth.md) for auth provider options.

!!! note
    See [Error Handling](error-handling.md) for the client exception hierarchy and example
    recovery patterns.
