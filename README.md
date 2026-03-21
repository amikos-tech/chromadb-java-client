# Chroma Vector Database Java Client

Production-ready Java client for ChromaDB v2 API.

This client works with Chroma Versions `>=1.0.0` | Requires Java 8+

## Requirements

- Java 8 or higher
- Maven or Gradle
- A running ChromaDB instance (`>=1.0.0`) or a Chroma Cloud account

## Installation

**Maven:**

```xml
<dependency>
    <groupId>io.github.amikos-tech</groupId>
    <artifactId>chromadb-java-client</artifactId>
    <version>0.2.0</version>
</dependency>
```

**Gradle:**

```gradle
implementation 'io.github.amikos-tech:chromadb-java-client:0.2.0'
```

## Quick Start

```java
import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;

Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .build();

DefaultEmbeddingFunction ef = new DefaultEmbeddingFunction();

Collection collection = client.getOrCreateCollection(
        "my-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ef)
                .build()
);

collection.add()
        .documents("Hello, my name is John. I am a Data Scientist.",
                   "Hello, my name is Bond. I am a Spy.")
        .ids("id-1", "id-2")
        .execute();

QueryResult result = collection.query()
        .queryTexts("Who is the spy?")
        .nResults(5)
        .include(Include.DOCUMENTS, Include.DISTANCES)
        .execute();

System.out.println(result);
```

## Usage

### Authentication

#### Basic Auth

```java
import tech.amikos.chromadb.v2.*;

Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .auth(BasicAuth.of("admin", "password"))
        .build();
```

#### Token Auth

```java
import tech.amikos.chromadb.v2.*;

Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .auth(TokenAuth.of(System.getenv("CHROMA_TOKEN")))
        .build();
```

#### Chroma Token Auth

```java
import tech.amikos.chromadb.v2.*;

Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .auth(ChromaTokenAuth.of(System.getenv("CHROMA_TOKEN")))
        .build();
```

### Cloud (Chroma Cloud)

```java
import tech.amikos.chromadb.v2.*;

Client client = ChromaClient.cloud()
        .apiKey(System.getenv("CHROMA_API_KEY"))
        .tenant(System.getenv("CHROMA_TENANT"))
        .database(System.getenv("CHROMA_DATABASE"))
        .build();
```

### Cloud vs Self-Hosted Feature Parity

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

### Transport Options

`ChromaClient.builder()` supports transport customization for production and platform integration scenarios:

```java
import okhttp3.OkHttpClient;
import tech.amikos.chromadb.v2.ChromaClient;
import tech.amikos.chromadb.v2.Client;

import java.nio.file.Paths;
import java.time.Duration;

// Custom CA certificate + env-based tenant/database
Client client = ChromaClient.builder()
        .baseUrl("https://your-chroma-host")
        .sslCert(Paths.get("/path/to/ca-cert.pem"))
        .tenantFromEnv("CHROMA_TENANT")
        .databaseFromEnv("CHROMA_DATABASE")
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(30))
        .build();

// Provide a fully configured OkHttpClient (mutually exclusive with builder timeout/TLS options)
OkHttpClient custom = new OkHttpClient.Builder()
        .readTimeout(Duration.ofSeconds(20))
        .build();

Client clientWithCustomHttp = ChromaClient.builder()
        .httpClient(custom)
        .build();
```

Notes:
- `.insecure(true)` enables trust-all TLS (development only).
- `.sslCert(...)` augments default JVM trust with your custom CA certificate(s).
- `.httpClient(...)` cannot be combined with `.connectTimeout(...)`, `.readTimeout(...)`, `.writeTimeout(...)`, `.sslCert(...)`, or `.insecure(...)`.
- `.tenantAndDatabaseFromEnv()` reads `CHROMA_TENANT` and `CHROMA_DATABASE`.

### Collection Lifecycle

```java
import tech.amikos.chromadb.v2.*;
import java.util.List;

// Create a new collection (throws ChromaConflictException if already exists)
Collection collection = client.createCollection("my-collection");

// Get or create (idempotent)
Collection collection = client.getOrCreateCollection("my-collection");

// Get an existing collection
Collection collection = client.getCollection("my-collection");

// List all collections
List<Collection> collections = client.listCollections();

// Delete a collection
client.deleteCollection("my-collection");

// Count collections
int count = client.countCollections();
```

### Adding Records

```java
import tech.amikos.chromadb.v2.*;
import java.util.HashMap;
import java.util.Map;

// Add with documents and metadata
Map<String, Object> meta1 = new HashMap<String, Object>();
meta1.put("type", "scientist");

Map<String, Object> meta2 = new HashMap<String, Object>();
meta2.put("type", "spy");

collection.add()
        .documents("Hello, my name is John. I am a Data Scientist.",
                   "Hello, my name is Bond. I am a Spy.")
        .metadatas(meta1, meta2)
        .ids("id-1", "id-2")
        .execute();

// Add with pre-computed embeddings
collection.add()
        .embeddings(new float[]{0.1f, 0.2f, 0.3f}, new float[]{0.4f, 0.5f, 0.6f})
        .ids("embed-1", "embed-2")
        .execute();
```

### Querying

```java
import tech.amikos.chromadb.v2.*;

// Query by text
QueryResult result = collection.query()
        .queryTexts("Who is the spy?")
        .nResults(5)
        .include(Include.DOCUMENTS, Include.DISTANCES)
        .execute();

// Query with metadata filter
QueryResult filtered = collection.query()
        .queryTexts("scientist")
        .nResults(5)
        .where(Where.eq("type", "scientist"))
        .execute();

// Query by pre-computed embeddings
QueryResult byEmbedding = collection.query()
        .queryEmbeddings(new float[]{0.1f, 0.2f, 0.3f})
        .nResults(3)
        .execute();
```

### Schema and CMEK

```java
import tech.amikos.chromadb.v2.*;

import java.util.Collections;

Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .build();

Schema schema = Schema.builder()
        .key(Schema.EMBEDDING_KEY, ValueTypes.builder()
                .floatList(FloatListValueType.builder()
                        .vectorIndex(VectorIndexType.builder()
                                .config(VectorIndexConfig.builder()
                                        .space(DistanceFunction.COSINE)
                                        .embeddingFunction(EmbeddingFunctionSpec.builder()
                                                .type("known")
                                                .name("openai")
                                                .config(Collections.<String, Object>singletonMap("api_key_env_var", "OPENAI_API_KEY"))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build())
        .cmek(Cmek.gcpKms("projects/my-project/locations/us-central1/keyRings/my-keyring/cryptoKeys/my-key"))
        .build();

Collection collection = client.getOrCreateCollection(
        "v2-schema-demo",
        CreateCollectionOptions.builder()
                .schema(schema)
                .build()
);

QueryResult result = collection.query()
        .queryTexts("find documents about transformers")
        .nResults(3)
        .include(Include.DOCUMENTS, Include.DISTANCES)
        .execute();
```

Notes:
- Runtime embedding function precedence: explicit runtime function passed during collection construction (`CreateCollectionOptions.embeddingFunction(...)` or `client.getCollection(name, embeddingFunction)`) wins.
- Descriptor fallback order when no runtime function is provided: `configuration.embedding_function`, then top-level `schema` `#embedding` vector index embedding function, then `configuration.schema` `#embedding` vector index embedding function.
- Unsupported providers in descriptors fail fast with `ChromaException` and guidance to use `queryEmbeddings(...)`.

### ID Generators

You can generate IDs client-side for `add()` and `upsert()` via `.idGenerator(...)`.

```java
import tech.amikos.chromadb.v2.*;

// Random UUID IDs (works with embeddings-only or documents)
collection.add()
        .idGenerator(UuidIdGenerator.INSTANCE)
        .embeddings(new float[]{1.0f, 2.0f}, new float[]{3.0f, 4.0f})
        .execute();

// ULID IDs
collection.upsert()
        .idGenerator(UlidIdGenerator.INSTANCE)
        .documents("doc-1", "doc-2")
        .execute();

// Deterministic IDs from document SHA-256
collection.add()
        .idGenerator(Sha256IdGenerator.INSTANCE)
        .documents("hello")
        .execute();
```

Rules:
- `ids(...)` and `idGenerator(...)` are mutually exclusive (validated at `execute()` time).
- `idGenerator(...)` requires at least one non-empty data field (`documents`, `embeddings`, `metadatas`, or `uris`) to infer record count.
- `Sha256IdGenerator` requires a non-null document or non-null metadata; throws `IllegalArgumentException` if both are null.
- Duplicate generated IDs within the same batch are rejected client-side before sending the request.

### Embedding Functions

#### Default (Local)

The default embedding function runs locally using ONNX Runtime and requires no API key.

```java
import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;

DefaultEmbeddingFunction ef = new DefaultEmbeddingFunction();

Collection collection = client.getOrCreateCollection(
        "my-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ef)
                .build()
);
```

#### OpenAI

Ensure `OPENAI_API_KEY` environment variable is set.

```java
import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

String apiKey = System.getenv("OPENAI_API_KEY");
OpenAIEmbeddingFunction ef = new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"));

Collection collection = client.getOrCreateCollection(
        "openai-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ef)
                .build()
);
```

#### Cohere

Ensure `COHERE_API_KEY` environment variable is set.

```java
import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.cohere.CohereEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

String apiKey = System.getenv("COHERE_API_KEY");
CohereEmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("embed-english-v2.0"));

Collection collection = client.getOrCreateCollection(
        "cohere-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ef)
                .build()
);
```

#### HuggingFace

Ensure `HF_API_KEY` environment variable is set.

```java
import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

String apiKey = System.getenv("HF_API_KEY");
HuggingFaceEmbeddingFunction ef = new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey));

Collection collection = client.getOrCreateCollection(
        "hf-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ef)
                .build()
);
```

For self-hosted HuggingFace Text Embeddings Inference (HFEI), start a local server first:

```bash
docker run -d -p 8008:80 --platform linux/amd64 --name hfei \
  ghcr.io/huggingface/text-embeddings-inference:cpu-1.8.3 \
  --model-id sentence-transformers/all-MiniLM-L6-v2
```

Then use the HFEI API type:

```java
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

HuggingFaceEmbeddingFunction ef = new HuggingFaceEmbeddingFunction(
        WithParam.baseAPI("http://localhost:8008"),
        new HuggingFaceEmbeddingFunction.WithAPIType(HuggingFaceEmbeddingFunction.APIType.HFEI_API));
```

#### Ollama

```java
import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction;

OllamaEmbeddingFunction ef = new OllamaEmbeddingFunction();

Collection collection = client.getOrCreateCollection(
        "ollama-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ef)
                .build()
);
```

## Status

**Supported:**
- v2 API (collections, records, queries, tenants, databases)
- Authentication: Basic Auth, Token (Bearer), Chroma Token, Chroma Cloud
- Embedding functions: Default/local (ONNX, no API key), OpenAI, Cohere, HuggingFace Inference API, HuggingFace Text Embeddings Inference (HFEI), Ollama
- ID generators: UUID, ULID, SHA-256
- Schema and CMEK (GCP KMS)
- Transport options: SSL certificates, custom timeouts, custom OkHttpClient
- Java 8+
- Chroma 1.0.0+

**Planned:**
- Async/reactive API
- Cloudflare Workers AI embeddings
- Gemini embeddings
- Observability hooks
- Spring integration

## Development

This project includes a `Makefile` to simplify common development tasks.

### Quick Start

```bash
# Display available commands
make help

# Build the project
make build

# Run tests
make test

# Create JAR package
make package
```

### Common Development Commands

| Command | Description |
|---------|-------------|
| `make build` | Clean and compile the project |
| `make test` | Run all tests |
| `make test-unit` | Run unit tests only |
| `make test-integration` | Run integration tests only |
| `make test-version CHROMA_VERSION=1.5.5` | Test with specific ChromaDB version |
| `make test-class TEST=YourTestClass` | Run specific test class |
| `make test-method TEST=YourTestClass#yourTestMethod` | Run specific test method |
| `make package` | Create JAR package |
| `make install` | Install to local Maven repository |
| `make clean` | Clean build artifacts |
| `make deps` | Download/update dependencies |
| `make deps-tree` | Display dependency tree |
| `make info` | Display project information |

### Environment Variables

For testing with external services, set these environment variables:
- `OPENAI_API_KEY` - Required for OpenAI embedding tests
- `COHERE_API_KEY` - Required for Cohere embedding tests
- `HF_API_KEY` - Required for HuggingFace embedding tests
- `CHROMA_VERSION` - Specify ChromaDB version for integration tests (default: `1.5.5`)

### Shortcuts

The Makefile also provides single-letter shortcuts for common commands:
- `make b` - Build
- `make t` - Test
- `make c` - Clean
- `make i` - Install

## Contributing

Pull requests are welcome.

## Upgrading from 0.1.x

Version 0.2.0 removes the v1 API classes and introduces a new builder-based v2 API. See [MIGRATION.md](MIGRATION.md) for breaking changes, a v1-to-v2 mapping table, and migration examples.

## References

- https://docs.trychroma.com/ - Official Chroma documentation
- https://github.com/amikos-tech/chromadb-chart - Chroma Helm chart for cloud-native deployments
- https://github.com/openai/openai-openapi - OpenAI OpenAPI specification

## Appendix: v1 API Examples (Legacy)

<details>
<summary>Expand v1 examples (deprecated)</summary>

> These examples use the removed v1 API. See [Quick Start](#quick-start) above for current v2 usage.

### Default Embedding Function (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            client.reset();
            EmbeddingFunction ef = new DefaultEmbeddingFunction();
            Collection collection = client.createCollection("test-collection", null, true, ef);
            List<Map<String, String>> metadata = new ArrayList<>();
            metadata.add(new HashMap<String, String>() {{
                put("type", "scientist");
            }});
            metadata.add(new HashMap<String, String>() {{
                put("type", "spy");
            }});
            collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
            Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
            System.out.println(qr);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

### OpenAI Embedding Function (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            String apiKey = System.getenv("OPENAI_API_KEY");
            EmbeddingFunction ef = new OpenAIEmbeddingFunction(WithParam.apiKey(apiKey), WithParam.model("text-embedding-3-small"));
            Collection collection = client.createCollection("test-collection", null, true, ef);
            List<Map<String, String>> metadata = new ArrayList<>();
            metadata.add(new HashMap<String, String>() {{
                put("type", "scientist");
            }});
            metadata.add(new HashMap<String, String>() {{
                put("type", "spy");
            }});
            collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
            Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
            System.out.println(qr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Cohere Embedding Function (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.cohere.CohereEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            client.reset();
            String apiKey = System.getenv("COHERE_API_KEY");
            EmbeddingFunction ef = new CohereEmbeddingFunction(WithParam.apiKey(apiKey));
            Collection collection = client.createCollection("test-collection", null, true, ef);
            List<Map<String, String>> metadata = new ArrayList<>();
            metadata.add(new HashMap<String, String>() {{
                put("type", "scientist");
            }});
            metadata.add(new HashMap<String, String>() {{
                put("type", "spy");
            }});
            collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
            Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
            System.out.println(qr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### HuggingFace Embedding Function (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client("http://localhost:8000");
            String apiKey = System.getenv("HF_API_KEY");
            EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(WithParam.apiKey(apiKey));
            Collection collection = client.createCollection("test-collection", null, true, ef);
            List<Map<String, String>> metadata = new ArrayList<>();
            metadata.add(new HashMap<String, String>() {{
                put("type", "scientist");
            }});
            metadata.add(new HashMap<String, String>() {{
                put("type", "spy");
            }});
            collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
            Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
            System.out.println(qr);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

### HuggingFace Text Embeddings Inference API (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client("http://localhost:8000");
            EmbeddingFunction ef = new HuggingFaceEmbeddingFunction(
                    WithParam.baseAPI("http://localhost:8008"),
                    new HuggingFaceEmbeddingFunction.WithAPIType(HuggingFaceEmbeddingFunction.APIType.HFEI_API));
            Collection collection = client.createCollection("test-collection", null, true, ef);
            List<Map<String, String>> metadata = new ArrayList<>();
            metadata.add(new HashMap<String, String>() {{
                put("type", "scientist");
            }});
            metadata.add(new HashMap<String, String>() {{
                put("type", "spy");
            }});
            collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
            Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
            System.out.println(qr);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

### Ollama Embedding Function (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            client.reset();
            EmbeddingFunction ef = new OllamaEmbeddingFunction();
            Collection collection = client.createCollection("test-collection", null, true, ef);
            List<Map<String, String>> metadata = new ArrayList<>();
            metadata.add(new HashMap<String, String>() {{
                put("type", "scientist");
            }});
            metadata.add(new HashMap<String, String>() {{
                put("type", "spy");
            }});
            collection.add(null, metadata, Arrays.asList("Hello, my name is John. I am a Data Scientist.", "Hello, my name is Bond. I am a Spy."), Arrays.asList("1", "2"));
            Collection.QueryResponse qr = collection.query(Arrays.asList("Who is the spy"), 10, null, null, null);
            System.out.println(qr);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

### Basic Auth (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            String encodedString = Base64.getEncoder().encodeToString("admin:admin".getBytes());
            client.setDefaultHeaders(new HashMap<String, String>() {{
                put("Authorization", "Basic " + encodedString);
            }});
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

### Token Auth (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            client.setDefaultHeaders(new HashMap<String, String>() {{
                put("Authorization", "Bearer test-token");
            }});
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

### X-Chroma-Token Auth (v1)

```java
package tech.amikos;

import tech.amikos.chromadb.*;
import tech.amikos.chromadb.Collection;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(System.getenv("CHROMA_URL"));
            client.setDefaultHeaders(new HashMap<String, String>() {{
                put("X-Chroma-Token", "test-token");
            }});
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
```

</details>
