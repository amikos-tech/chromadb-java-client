import tech.amikos.chromadb.v2.*;

public class IdGeneratorsExample {

    public static void main(String[] args) {
        Client client = ChromaClient.builder()
                .baseUrl(System.getenv("CHROMA_URL"))
                .build();

        Collection collection = client.getOrCreateCollection("id-gen-demo");

        // --8<-- [start:uuid]
        collection.add()
                .idGenerator(UuidIdGenerator.INSTANCE)
                .embeddings(new float[]{1.0f, 2.0f}, new float[]{3.0f, 4.0f})
                .execute();
        // --8<-- [end:uuid]

        // --8<-- [start:ulid]
        collection.upsert()
                .idGenerator(UlidIdGenerator.INSTANCE)
                .documents("doc-1", "doc-2")
                .execute();
        // --8<-- [end:ulid]

        // --8<-- [start:sha256]
        collection.add()
                .idGenerator(Sha256IdGenerator.INSTANCE)
                .documents("hello")
                .execute();
        // --8<-- [end:sha256]
    }
}
