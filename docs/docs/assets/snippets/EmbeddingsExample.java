import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;
import tech.amikos.chromadb.embeddings.cohere.CohereEmbeddingFunction;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;
import tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction;

// --8<-- [start:default]
// No API key required. Uses ONNX Runtime for local inference.
// Model is downloaded on first use.
DefaultEmbeddingFunction ef = new DefaultEmbeddingFunction();

Collection collection = client.getOrCreateCollection(
        "my-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ef)
                .build()
);
// --8<-- [end:default]

// --8<-- [start:openai]
// Requires OPENAI_API_KEY environment variable
OpenAIEmbeddingFunction openaiEf = new OpenAIEmbeddingFunction(
        WithParam.apiKey(System.getenv("OPENAI_API_KEY")),
        WithParam.model("text-embedding-3-small")
);

Collection openaiCollection = client.getOrCreateCollection(
        "openai-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(openaiEf)
                .build()
);
// --8<-- [end:openai]

// --8<-- [start:cohere]
// Requires COHERE_API_KEY environment variable
CohereEmbeddingFunction cohereEf = new CohereEmbeddingFunction(
        WithParam.apiKey(System.getenv("COHERE_API_KEY")),
        WithParam.model("embed-english-v2.0")
);

Collection cohereCollection = client.getOrCreateCollection(
        "cohere-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(cohereEf)
                .build()
);
// --8<-- [end:cohere]

// --8<-- [start:huggingface]
// Requires HF_API_KEY environment variable
HuggingFaceEmbeddingFunction hfEf = new HuggingFaceEmbeddingFunction(
        WithParam.apiKey(System.getenv("HF_API_KEY"))
);

Collection hfCollection = client.getOrCreateCollection(
        "hf-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(hfEf)
                .build()
);
// --8<-- [end:huggingface]

// --8<-- [start:huggingface-hfei]
// Self-hosted HuggingFace Text Embeddings Inference (HFEI)
HuggingFaceEmbeddingFunction hfeiEf = new HuggingFaceEmbeddingFunction(
        WithParam.baseAPI("http://localhost:8008"),
        new HuggingFaceEmbeddingFunction.WithAPIType(HuggingFaceEmbeddingFunction.APIType.HFEI_API)
);

Collection hfeiCollection = client.getOrCreateCollection(
        "hfei-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(hfeiEf)
                .build()
);
// --8<-- [end:huggingface-hfei]

// --8<-- [start:ollama]
// Requires Ollama running at http://localhost:11434 with nomic-embed-text model pulled
OllamaEmbeddingFunction ollamaEf = new OllamaEmbeddingFunction();

Collection ollamaCollection = client.getOrCreateCollection(
        "ollama-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ollamaEf)
                .build()
);
// --8<-- [end:ollama]
