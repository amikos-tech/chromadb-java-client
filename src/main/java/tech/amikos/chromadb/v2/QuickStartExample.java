package tech.amikos.chromadb.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * QuickStart example demonstrating the V2 API with radical simplicity.
 * Shows both convenience methods (for simple cases) and builders (for complex cases).
 */
public class QuickStartExample {
    public static void main(String[] args) {
        // Single client class with builder
        ChromaClient client = ChromaClient.builder()
                .serverUrl("http://localhost:8000")
                .auth(AuthProvider.none())
                .build();

        // Simple collection creation
        Collection collection = client.createCollection("my-collection",
                Map.of("description", "Example collection"));

        System.out.println("=== SIMPLE API (Convenience Methods) ===\n");

        // Simple add - Chroma-aligned API (most common use case)
        collection.add(
                List.of("id1", "id2", "id3"),
                List.of(
                        List.of(0.1f, 0.2f, 0.3f),
                        List.of(0.4f, 0.5f, 0.6f),
                        List.of(0.7f, 0.8f, 0.9f)
                ),
                List.of(
                        "This is a document about technology",
                        "This is a blog post about science",
                        "This is an article about technology"
                ),
                List.of(
                        Map.of("type", "article", "category", "tech"),
                        Map.of("type", "blog", "category", "science"),
                        Map.of("type", "article", "category", "tech")
                )
        );

        System.out.println("Added " + collection.count() + " records\n");

        // Simple query by embeddings
        QueryResponse results = collection.query(
                List.of(List.of(0.1f, 0.2f, 0.3f)),
                2
        );
        System.out.println("Simple query results: " + results.getIds());

        // Simple query with filtering
        results = collection.query(
                List.of(List.of(0.1f, 0.2f, 0.3f)),
                2,
                Where.eq("type", "article")
        );
        System.out.println("Filtered query results: " + results.getIds());

        // Simple query by text (auto-embedded by collection's embedding function)
        QueryResponse textResults = collection.queryByText(
                List.of("technology innovation"),
                5
        );
        System.out.println("Text query results: " + textResults.getIds());

        // Simple get by IDs
        GetResponse records = collection.get(List.of("id1", "id2"));
        System.out.println("Got records: " + records.getIds());

        // Simple get with includes
        records = collection.get(
                List.of("id1", "id2"),
                Include.DOCUMENTS, Include.METADATAS
        );
        System.out.println("Got records with data: " + records.getDocuments());

        System.out.println("\n=== ADVANCED API (Builder Pattern) ===\n");

        // Complex query using builder - for when you need all the options
        QueryResponse complexResults = collection.query()
                .queryEmbeddings(List.of(List.of(0.1f, 0.2f, 0.3f)))
                .nResults(2)
                .where(Where.eq("type", "article"))
                .whereDocument(WhereDocument.contains("technology"))
                .include(Include.DOCUMENTS, Include.DISTANCES, Include.METADATAS)
                .execute();

        System.out.println("Complex query results: " + complexResults.getIds());

        // Complex get with filtering and pagination
        GetResponse filteredRecords = collection.get()
                .where(Where.eq("category", "tech"))
                .limit(10)
                .offset(0)
                .include(Include.DOCUMENTS, Include.METADATAS)
                .execute();

        System.out.println("Filtered get results: " + filteredRecords.getIds());

        // Update using builder (complex case)
        collection.update()
                .ids(List.of("id1"))
                .metadatas(List.of(Map.of("type", "article", "category", "tech", "featured", true)))
                .execute();

        System.out.println("Updated record metadata");

        // Upsert with convenience method
        collection.upsert(
                List.of("id4"),
                List.of(List.of(0.2f, 0.3f, 0.4f)),
                List.of("New document about AI")
        );
        System.out.println("Upserted new record");

        System.out.println("\n=== CLEANUP ===\n");

        // Simple delete by IDs
        collection.delete(List.of("id4"));
        System.out.println("Deleted id4");

        // Delete with filtering using convenience method
        collection.delete(Where.eq("type", "blog"));
        System.out.println("Deleted all blog posts");

        System.out.println("Final count: " + collection.count() + " records remaining");

        System.out.println("\n=== OTHER FEATURES ===\n");

        // Strongly-typed Metadata
        Metadata metadata = Metadata.builder()
                .putString("description", "Updated collection")
                .putInt("version", 2)
                .putList("tags", List.of("ai", "vectors", "search"))
                .build();

        System.out.println("Created metadata: " + metadata.toMap());

        // Cloud mode example
        ChromaClient cloudClient = ChromaClient.builder()
                .cloudUrl("https://api.trychroma.com")
                .apiKey("your-api-key")
                .tenant("my-tenant")
                .database("my-database")
                .build();

        System.out.println("\n=== KEY TAKEAWAYS ===");
        System.out.println("1. Use convenience methods for 80% of use cases (simple & Chroma-aligned)");
        System.out.println("2. Use builders when you need advanced filtering or options");
        System.out.println("3. Both approaches work together seamlessly!");
    }
}