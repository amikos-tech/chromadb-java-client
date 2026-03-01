package tech.amikos.chromadb.v2;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.*;

public class ErrorHandlingIntegrationTest extends AbstractChromaIntegrationTest {

    // --- duplicate collection → 409 ---

    @Test
    public void testDuplicateCollectionThrowsConflict() {
        client.createCollection("conflict_col");
        try {
            client.createCollection("conflict_col");
            fail("Expected ChromaConflictException");
        } catch (ChromaConflictException e) {
            assertEquals(409, e.getStatusCode());
        }
    }

    // --- nonexistent collection → 404 ---

    @Test
    public void testNonexistentCollectionThrowsNotFound() {
        try {
            client.getCollection("nonexistent_col");
            fail("Expected ChromaNotFoundException");
        } catch (ChromaNotFoundException e) {
            assertEquals(404, e.getStatusCode());
        }
    }

    // --- exception hierarchy: instanceof ChromaClientException ---

    @Test
    public void testConflictIsInstanceOfClientException() {
        client.createCollection("hierarchy_col");
        try {
            client.createCollection("hierarchy_col");
            fail("Expected ChromaConflictException");
        } catch (ChromaConflictException e) {
            assertTrue(e instanceof ChromaClientException);
            assertTrue(e instanceof ChromaException);
        }
    }

    // --- not found is instanceof ChromaClientException ---

    @Test
    public void testNotFoundIsInstanceOfClientException() {
        try {
            client.getCollection("missing_col");
            fail("Expected ChromaNotFoundException");
        } catch (ChromaNotFoundException e) {
            assertTrue(e instanceof ChromaClientException);
            assertTrue(e instanceof ChromaException);
        }
    }

    // --- verify status codes on exceptions ---

    @Test
    public void testExceptionCarriesStatusCode() {
        try {
            client.getCollection("absent_col");
            fail("Expected ChromaNotFoundException");
        } catch (ChromaNotFoundException e) {
            assertTrue(e.hasStatusCode());
            assertEquals(404, e.getStatusCode());
        }
    }

    // --- connection to bad URL → ChromaConnectionException ---

    @Test(expected = ChromaConnectionException.class)
    public void testConnectionToBadUrlThrowsConnectionException() {
        Client badClient = ChromaClient.builder()
                .baseUrl("http://localhost:1")
                .connectTimeout(Duration.ofSeconds(1))
                .build();
        try {
            badClient.heartbeat();
        } finally {
            badClient.close();
        }
    }

}
