# Authentication

The ChromaDB Java client supports three authentication methods for self-hosted deployments (Basic
Auth, Token Auth, Chroma Token Auth) and API key authentication for Chroma Cloud.

All auth providers are passed to the builder via `.auth(...)` and apply their credentials to
every HTTP request.

## Basic Auth

HTTP Basic authentication encodes the username and password as a base64 `Authorization` header:

```java
--8<-- "AuthExample.java:basic"
```

`BasicAuth.of(username, password)` constructs the provider. Both values must be non-null and
non-blank.

## Token Auth

Bearer token authentication sends the token as `Authorization: Bearer <token>`:

```java
--8<-- "AuthExample.java:token"
```

`TokenAuth.of(token)` constructs the provider. The token must be non-null and non-blank.

## Chroma Token Auth

Chroma Token Auth sends the token as the `X-Chroma-Token` header (compatible with Chroma's
built-in token authentication backend):

```java
--8<-- "AuthExample.java:chroma-token"
```

`ChromaTokenAuth.of(token)` constructs the provider. The token must be non-null and non-blank.

## Chroma Cloud

Chroma Cloud uses API key authentication via the dedicated `ChromaClient.cloud()` builder:

```java
--8<-- "AuthExample.java:cloud"
```

The cloud builder handles authentication internally. Do not call `.auth(...)` on a cloud client —
the API key is passed as a dedicated parameter.

!!! warning
    Only one auth method per client. Setting auth twice throws `IllegalStateException`. If you need
    to switch credentials, create a new client instance.

!!! tip
    Auth providers use factory methods. Use `BasicAuth.of()`, `TokenAuth.of()`, and
    `ChromaTokenAuth.of()` — constructors are private.
