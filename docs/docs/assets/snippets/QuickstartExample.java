import tech.amikos.chromadb.v2.*;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;

// --8<-- [start:full]
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
// --8<-- [end:full]
