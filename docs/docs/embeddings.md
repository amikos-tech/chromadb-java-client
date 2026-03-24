# Embeddings

Embedding functions convert text or data into numeric vectors that ChromaDB stores and searches.
Multiple providers are supported, from local inference (no API key) to cloud API providers.

| Provider | API Key Required | Local | Notes |
|---|---|---|---|
| Default (ONNX) | No | Yes | Downloads model on first use |
| OpenAI | Yes | No | `OPENAI_API_KEY` |
| Cohere | Yes | No | `COHERE_API_KEY` |
| HuggingFace Inference API | Yes | No | `HF_API_KEY` |
| HuggingFace TEI (self-hosted) | No | Yes | Docker required |
| Ollama | No | Yes | Ollama daemon required |

!!! tip
    All embedding constructors use `WithParam` factory methods. Import
    `tech.amikos.chromadb.embeddings.WithParam`.

## Default (Local ONNX)

The default embedding function runs locally using ONNX Runtime. No API key is required:

```java
--8<-- "EmbeddingsExample.java:default"
```

The model (`all-MiniLM-L6-v2`) is downloaded automatically to a local cache on first use.
No network calls are made during embedding after the initial download.

## OpenAI

```java
--8<-- "EmbeddingsExample.java:openai"
```

!!! note
    Set the `OPENAI_API_KEY` environment variable before running. Refer to the
    [OpenAI API documentation](https://platform.openai.com/docs/api-reference/embeddings) for
    available model names.

## Cohere

```java
--8<-- "EmbeddingsExample.java:cohere"
```

!!! note
    Set the `COHERE_API_KEY` environment variable before running. Refer to the
    [Cohere API documentation](https://docs.cohere.com/reference/embed) for available model names.

## HuggingFace Inference API

```java
--8<-- "EmbeddingsExample.java:huggingface"
```

!!! note
    Set the `HF_API_KEY` environment variable before running. The default model is
    `sentence-transformers/all-MiniLM-L6-v2`.

## HuggingFace TEI (Self-Hosted)

Start a local Text Embeddings Inference server with Docker:

```bash
docker run -d -p 8008:80 --platform linux/amd64 --name hfei \
  ghcr.io/huggingface/text-embeddings-inference:cpu-1.8.3 \
  --model-id sentence-transformers/all-MiniLM-L6-v2
```

Then connect using the HFEI API type:

```java
--8<-- "EmbeddingsExample.java:huggingface-hfei"
```

## Ollama

```java
--8<-- "EmbeddingsExample.java:ollama"
```

Ollama must be running locally with the `nomic-embed-text` model pulled:

```bash
ollama pull nomic-embed-text
```

Use `WithParam.baseAPI(url)` and `WithParam.model(name)` to customize the endpoint and model.

## Embedding Function Precedence

When multiple embedding function sources are present, the client resolves them in the following
order (highest to lowest priority):

1. **Explicit runtime EF** — set via `CreateCollectionOptions.embeddingFunction(ef)` or
   `client.getCollection(name, embeddingFunction)`. Always wins.
2. **`configuration.embedding_function`** — persisted in the collection's configuration descriptor.
3. **`schema.default_embedding_function`** — persisted in the collection's schema descriptor.

When an explicit runtime EF overrides a persisted EF descriptor, a `WARNING` log is emitted.
No error is thrown — the explicit EF is used.
