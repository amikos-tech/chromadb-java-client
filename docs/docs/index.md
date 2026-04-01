# ChromaDB Java Client

Production-ready Java client for ChromaDB v2 API.

Works with Chroma `>=1.0.0` | Requires Java 8+

[Get Started](client.md){ .md-button .md-button--primary }
[View on GitHub](https://github.com/amikos-tech/chromadb-java-client){ .md-button }

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
--8<-- "QuickstartExample.java:full"
```

## Features

- **Multiple Auth Methods** — Basic Auth, Bearer Token, Chroma Token
- **Cloud & Self-Hosted** — Works with Chroma Cloud and self-hosted instances
- **Fluent API** — Builder pattern for all operations
- **Type-Safe Filters** — `Where` and `WhereDocument` DSL
- **Search API** — KNN, RRF ranking, field projection, groupBy
- **Embedding Providers** — OpenAI, Cohere, HuggingFace, Ollama, local ONNX
- **Schema & CMEK** — Full schema control and encryption key management
- **ID Generators** — UUID, ULID, SHA-256 content-based IDs
