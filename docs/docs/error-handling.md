# Error Handling

All client exceptions are unchecked and inherit from `ChromaException`, so the compiler will not
force `try/catch` blocks for you. Catch the specific cases you can recover from, and let the rest
bubble up to your application's error boundary.

## Exception Categories

| Type | Typical cause |
|------|---------------|
| `ChromaConnectionException` | Network failure, timeout, DNS issue, or unreachable server |
| `ChromaDeserializationException` | The server returned a successful response the client could not parse |
| `ChromaBadRequestException` | Invalid request payload or unsupported filter/search shape |
| `ChromaUnauthorizedException` | Missing or invalid credentials |
| `ChromaForbiddenException` | Authenticated, but not allowed to perform the operation |
| `ChromaNotFoundException` | Missing tenant, database, collection, or unsupported endpoint |
| `ChromaConflictException` | Resource already exists |
| `ChromaClientException` | Other client-side 4xx errors not covered by a specific subclass (for example 429 rate limiting) |
| `ChromaServerException` | Server-side 5xx failure |

## Recoverable Patterns

Handle common startup and lifecycle failures close to the call site:

```java
--8<-- "ErrorHandlingExample.java:create-collection"
```

Use `ChromaNotFoundException` when missing resources are an expected control-flow branch:

```java
--8<-- "ErrorHandlingExample.java:get-collection"
```

## Request Validation

Builder APIs still fail at runtime if the request is invalid for the server or client-side
validation rules:

```java
--8<-- "ErrorHandlingExample.java:bad-request"
```

## Catch-All Boundary

If you need a broader boundary, catch specific auth/server failures first and then fall back to
`ChromaException`:

```java
--8<-- "ErrorHandlingExample.java:catch-all"
```

## Recommendations

- Catch the narrowest exception you can recover from.
- Treat `ChromaConnectionException` as retryable only when the operation itself is safe to retry.
- Log `ChromaServerException` with request context so 5xx failures can be correlated server-side.
- Validate required environment variables before constructing clients in runnable programs.
