import tech.amikos.chromadb.v2.*;

public class CloudExample {

    public static void main(String[] args) {
        // --8<-- [start:cloud-client]
        Client client = ChromaClient.cloud()
                .apiKey(System.getenv("CHROMA_API_KEY"))
                .tenant(System.getenv("CHROMA_TENANT"))
                .database(System.getenv("CHROMA_DATABASE"))
                .build();
        // --8<-- [end:cloud-client]

        Collection collection = client.getOrCreateCollection("my-collection");

        // --8<-- [start:fork]
        Collection forked = collection.fork("forked-collection");
        // --8<-- [end:fork]

        // --8<-- [start:fork-count]
        int forkCount = collection.forkCount();
        // --8<-- [end:fork-count]

        // --8<-- [start:indexing-status]
        IndexingStatus status = collection.indexingStatus();
        long indexed = status.getNumIndexedOps();
        long unindexed = status.getNumUnindexedOps();
        long total = status.getTotalOps();
        double progress = status.getOpIndexingProgress();
        // --8<-- [end:indexing-status]
    }
}
