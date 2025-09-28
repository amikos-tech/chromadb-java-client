package tech.amikos.chromadb.v2.examples;

import tech.amikos.chromadb.v2.auth.AuthProvider;
import tech.amikos.chromadb.v2.client.Collection;
import tech.amikos.chromadb.v2.client.ServerClient;
import tech.amikos.chromadb.v2.model.*;

import java.util.List;
import java.util.Map;

public class QuickStartExample {
    public static void main(String[] args) {
        ServerClient client = ServerClient.builder()
                .baseUrl("http://localhost:8000")
                .auth(AuthProvider.none())
                .build();

        Collection collection = client.createCollection("my-collection", config -> config
                .metadata(Map.of("description", "Example collection"))
        );

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

        QueryResponse results = collection.query()
                .queryEmbeddings(List.of(List.of(0.1f, 0.2f, 0.3f)))
                .nResults(2)
                .where(Where.eq("type", "article"))
                .include(Include.DOCUMENTS, Include.DISTANCES, Include.METADATAS)
                .execute();

        System.out.println("Query results: " + results.getIds());

        GetResponse allRecords = collection.get()
                .where(Where.eq("category", "tech"))
                .include(Include.DOCUMENTS, Include.METADATAS)
                .execute();

        System.out.println("Tech articles: " + allRecords.getIds());

        collection.delete()
                .where(Where.eq("type", "blog"))
                .execute();

        System.out.println("After deletion: " + collection.count() + " records remaining");
    }
}