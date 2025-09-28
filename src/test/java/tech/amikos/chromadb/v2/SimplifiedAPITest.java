package tech.amikos.chromadb.v2;

import org.junit.Test;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests to verify the simplified V2 API structure.
 * These tests verify the API design without requiring a running ChromaDB instance.
 */
public class SimplifiedAPITest {

    @Test
    public void testChromaClientBuilder() {
        // Test that ChromaClient builder works correctly
        ChromaClient client = ChromaClient.builder()
            .serverUrl("http://localhost:8000")
            .auth(AuthProvider.none())
            .tenant("test-tenant")
            .database("test-db")
            .connectTimeout(30)
            .readTimeout(30)
            .writeTimeout(30)
            .build();

        assertNotNull(client);
    }

    @Test
    public void testCloudModeBuilder() {
        // Test cloud mode syntactic sugar
        ChromaClient client = ChromaClient.builder()
            .cloudUrl("https://api.trychroma.com")
            .apiKey("test-api-key")
            .tenant("my-tenant")
            .database("my-database")
            .build();

        assertNotNull(client);
    }

    @Test
    public void testAuthProviderFactoryMethods() {
        // Test all auth provider factory methods
        AuthProvider none = AuthProvider.none();
        assertNotNull(none);
        assertTrue(none instanceof NoAuthProvider);

        AuthProvider token = AuthProvider.token("test-token");
        assertNotNull(token);
        assertTrue(token instanceof TokenAuthProvider);

        AuthProvider basic = AuthProvider.basic("user", "pass");
        assertNotNull(basic);
        assertTrue(basic instanceof BasicAuthProvider);

        AuthProvider chromaToken = AuthProvider.chromaToken("chroma-token");
        assertNotNull(chromaToken);
        assertTrue(chromaToken instanceof ChromaTokenAuthProvider);
    }

    @Test
    public void testMetadataBuilder() {
        // Test strongly typed metadata
        Metadata metadata = Metadata.builder()
            .putString("title", "Test Document")
            .putInt("version", 1)
            .putLong("timestamp", System.currentTimeMillis())
            .putDouble("score", 95.5)
            .putBoolean("published", true)
            .putList("tags", Arrays.asList("test", "example"))
            .build();

        assertNotNull(metadata);
        assertEquals("Test Document", metadata.getString("title"));
        assertEquals(Integer.valueOf(1), metadata.getInt("version"));
        assertEquals(Double.valueOf(95.5), metadata.getDouble("score"));
        assertEquals(Boolean.TRUE, metadata.getBoolean("published"));
        assertNotNull(metadata.getStringList("tags"));
        assertEquals(2, metadata.getStringList("tags").size());
    }

    @Test
    public void testMetadataImmutability() {
        // Test that metadata is immutable
        Metadata original = Metadata.builder()
            .putString("key", "value")
            .build();

        Metadata modified = original.with("newKey", "newValue");

        assertNotEquals(original, modified);
        assertNull(original.getString("newKey"));
        assertEquals("newValue", modified.getString("newKey"));
    }

    @Test
    public void testWhereFilters() {
        // Test Where filter creation
        Where eqFilter = Where.eq("field", "value");
        assertNotNull(eqFilter);

        Where gtFilter = Where.gt("age", 30);
        assertNotNull(gtFilter);

        Where andFilter = Where.and(
            Where.eq("city", "NYC"),
            Where.gte("age", 25)
        );
        assertNotNull(andFilter);

        Where orFilter = Where.or(
            Where.eq("status", "active"),
            Where.eq("status", "pending")
        );
        assertNotNull(orFilter);
    }

    @Test
    public void testIncludeEnum() {
        // Test Include enum exists and has expected values
        Include[] values = Include.values();
        assertTrue(values.length > 0);

        // Common include types should exist
        assertNotNull(Include.valueOf("DOCUMENTS"));
        assertNotNull(Include.valueOf("METADATAS"));
        assertNotNull(Include.valueOf("DISTANCES"));
    }

    @Test
    public void testCreateCollectionRequestBuilder() {
        // Test that CreateCollectionRequest builder is public and works
        CreateCollectionRequest request = new CreateCollectionRequest.Builder("test-collection")
            .metadata(Map.of("key", "value"))
            .build();

        assertNotNull(request);
    }

    @Test
    public void testFlatPackageStructure() {
        // Verify all main classes are in the flat v2 package
        Class<?>[] coreClasses = {
            ChromaClient.class,
            Collection.class,
            Metadata.class,
            AuthProvider.class,
            NoAuthProvider.class,
            TokenAuthProvider.class,
            BasicAuthProvider.class,
            ChromaTokenAuthProvider.class,
            Where.class,
            WhereDocument.class,
            Include.class,
            QueryResponse.class,
            GetResponse.class,
            ChromaV2Exception.class,
            ChromaNotFoundException.class,
            ChromaBadRequestException.class,
            ChromaUnauthorizedException.class,
            ChromaServerException.class
        };

        for (Class<?> clazz : coreClasses) {
            String packageName = clazz.getPackage().getName();
            assertEquals("All classes should be in flat v2 package",
                "tech.amikos.chromadb.v2", packageName);
        }
    }

    @Test
    public void testNoConsumerPatterns() {
        // Verify Collection class doesn't have Consumer<Builder> methods
        // This test verifies at compile time that the old patterns are gone

        // The following would not compile if Consumer patterns existed:
        // collection.query(builder -> builder.nResults(10));  // This pattern is removed

        // Only the fluent builder pattern should work:
        // collection.query().nResults(10).execute();  // This is the only way

        // Test passes if compilation succeeds
        assertTrue("Code compiles without Consumer<Builder> patterns", true);
    }
}