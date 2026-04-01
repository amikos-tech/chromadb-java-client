import tech.amikos.chromadb.v2.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ErrorHandlingExample {

    public static void main(String[] args) {
        Client client = ChromaClient.builder()
                .baseUrl(requiredEnv("CHROMA_URL"))
                .build();

        // --8<-- [start:create-collection]
        try {
            client.createCollection("my-collection");
        } catch (ChromaConflictException e) {
            System.out.println("Collection already exists: " + e.getMessage());
        } catch (ChromaConnectionException e) {
            System.err.println("Could not reach Chroma: " + e.getMessage());
        }
        // --8<-- [end:create-collection]

        // --8<-- [start:get-collection]
        try {
            Collection collection = client.getCollection("my-collection");
            System.out.println("Collection ID: " + collection.getId());
        } catch (ChromaNotFoundException e) {
            System.out.println("Collection not found: " + e.getMessage());
        }
        // --8<-- [end:get-collection]

        Collection collection = client.getOrCreateCollection("my-collection");

        // --8<-- [start:bad-request]
        Map<String, Object> invalidMetadata = new HashMap<String, Object>();
        invalidMetadata.put("tags", Arrays.<Object>asList("ok", 1));

        try {
            collection.add()
                    .ids("id-1")
                    .documents("Hello, world!")
                    .metadatas(Collections.<Map<String, Object>>singletonList(invalidMetadata))
                    .execute();
        } catch (ChromaBadRequestException e) {
            System.out.println("Request validation failed: " + e.getMessage());
        }
        // --8<-- [end:bad-request]

        // --8<-- [start:catch-all]
        try {
            client.heartbeat();
        } catch (ChromaUnauthorizedException e) {
            System.err.println("Check your credentials: " + e.getMessage());
        } catch (ChromaServerException e) {
            System.err.println("Chroma returned a server error: " + e.getMessage());
        } catch (ChromaException e) {
            System.err.println("Other Chroma client failure: " + e.getMessage());
        }
        // --8<-- [end:catch-all]
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }
}
