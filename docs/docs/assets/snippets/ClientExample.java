import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;

import java.util.List;

// --8<-- [start:self-hosted]
Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .build();
// --8<-- [end:self-hosted]

// --8<-- [start:cloud]
Client cloudClient = ChromaClient.cloud()
        .apiKey(System.getenv("CHROMA_API_KEY"))
        .tenant(System.getenv("CHROMA_TENANT"))
        .database(System.getenv("CHROMA_DATABASE"))
        .build();
// --8<-- [end:cloud]

// --8<-- [start:lifecycle]
// Create a new collection (throws ChromaConflictException if already exists)
Collection collection = client.createCollection("my-collection");

// Get or create (idempotent)
Collection col2 = client.getOrCreateCollection("my-collection");

// Get an existing collection
Collection col3 = client.getCollection("my-collection");

// List all collections
List<Collection> collections = client.listCollections();

// Delete a collection
client.deleteCollection("my-collection");

// Count collections
int count = client.countCollections();
// --8<-- [end:lifecycle]

// --8<-- [start:with-ef]
DefaultEmbeddingFunction ef = new DefaultEmbeddingFunction();

Collection embCol = client.getOrCreateCollection(
        "my-collection",
        CreateCollectionOptions.builder()
                .embeddingFunction(ef)
                .build()
);
// --8<-- [end:with-ef]

// --8<-- [start:health]
client.heartbeat();
String version = client.version();
// --8<-- [end:health]
