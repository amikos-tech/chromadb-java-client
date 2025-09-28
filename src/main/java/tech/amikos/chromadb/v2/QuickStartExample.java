package tech.amikos.chromadb.v2;

import java.util.List;
import java.util.Map;

/**
 * QuickStart example demonstrating the V2 API with radical simplicity.
 * Shows the single way to accomplish each task using builder patterns.
 */
public class QuickStartExample {
    public static void main(String[] args) {
        // Single client class with builder
        ChromaClient client = ChromaClient.builder()
                .serverUrl("http://localhost:8000")
                .auth(AuthProvider.none())
                .build();

        // Simple collection creation with metadata
        Collection collection = client.createCollection("my-collection",
                Map.of("description", "Example collection"));

        // Add records using fluent builder pattern
        collection.add()
                .ids(List.of("id1", "id2", "id3"))
                .embeddings(List.of(
                        List.of(0.1f, 0.2f, 0.3f),
                        List.of(0.4f, 0.5f, 0.6f),
                        List.of(0.7f, 0.8f, 0.9f)
                ))
                .documents(List.of(
                        "This is a document about technology",
                        "This is a blog post about science",
                        "This is an article about technology"
                ))
                .metadatas(List.of(
                        Map.of("type", "article", "category", "tech"),
                        Map.of("type", "blog", "category", "science"),
                        Map.of("type", "article", "category", "tech")
                ))
                .execute();

        System.out.println("Added " + collection.count() + " records");

        // Query using fluent builder - single approach, no Consumer<Builder>
        QueryResponse results = collection.query()
                .queryEmbeddings(List.of(List.of(0.1f, 0.2f, 0.3f)))
                .nResults(2)
                .where(Where.eq("type", "article"))
                .include(Include.DOCUMENTS, Include.DISTANCES, Include.METADATAS)
                .execute();

        System.out.println("Query results: " + results.getIds());

        // Get records with filtering
        GetResponse allRecords = collection.get()
                .where(Where.eq("category", "tech"))
                .include(Include.DOCUMENTS, Include.METADATAS)
                .execute();

        System.out.println("Tech articles: " + allRecords.getIds());

        // Delete records with filtering
        collection.delete()
                .where(Where.eq("type", "blog"))
                .execute();

        System.out.println("After deletion: " + collection.count() + " records remaining");

        // Demonstrate strongly-typed Metadata usage
        Metadata metadata = Metadata.builder()
                .putString("description", "Updated collection")
                .putInt("version", 2)
                .putList("tags", List.of("ai", "vectors", "search"))
                .build();

        // Example of cloud mode (syntactic sugar)
        ChromaClient cloudClient = ChromaClient.builder()
                .cloudUrl("https://api.trychroma.com")
                .apiKey("your-api-key")
                .tenant("my-tenant")
                .database("my-database")
                .build();
    }
}