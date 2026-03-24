# Transport Options

`ChromaClient.builder()` supports transport customization for production and platform integration.

## SSL Certificates

Use `.sslCert(Path)` to provide a custom CA certificate. This augments the default JVM trust store with your certificate(s), enabling connections to Chroma instances using a private CA.

```java
--8<-- "TransportExample.java:ssl-cert"
```

The `Path` argument points to a PEM-encoded CA certificate file.

## Custom Timeouts

Configure per-direction timeouts with `.connectTimeout(...)`, `.readTimeout(...)`, and `.writeTimeout(...)`.

```java
--8<-- "TransportExample.java:custom-timeouts"
```

All timeout methods accept `java.time.Duration`. If only `.readTimeout(...)` is set, connect and write timeouts use OkHttp defaults.

## Custom OkHttpClient

Pass a fully configured `OkHttpClient` instance via `.httpClient(...)` for advanced scenarios (connection pooling, interceptors, proxy configuration).

```java
--8<-- "TransportExample.java:custom-http"
```

!!! warning
    `.httpClient(...)` cannot be combined with `.connectTimeout(...)`, `.readTimeout(...)`, `.writeTimeout(...)`, `.sslCert(...)`, or `.insecure(...)`. An `IllegalStateException` is thrown at build time if both are set.

## Insecure TLS (Development Only)

`.insecure(true)` enables trust-all TLS with hostname verification disabled. This is useful for local development with self-signed certificates.

```java
--8<-- "TransportExample.java:insecure"
```

!!! warning
    `.insecure(true)` accepts any TLS certificate without validation. Never use in production environments.

## Environment-Based Tenant/Database

Read tenant and database names from environment variables at build time:

```java
--8<-- "TransportExample.java:env-tenant"
```

Convenience methods:

- `.tenantFromEnv(envVar)` — reads the named environment variable as the tenant
- `.databaseFromEnv(envVar)` — reads the named environment variable as the database
- `.tenantAndDatabaseFromEnv()` — shorthand for `.tenantFromEnv("CHROMA_TENANT").databaseFromEnv("CHROMA_DATABASE")`

## Full Example

Combining SSL certificate, environment-based tenant/database, and custom timeouts:

```java
--8<-- "TransportExample.java:full-example"
```
