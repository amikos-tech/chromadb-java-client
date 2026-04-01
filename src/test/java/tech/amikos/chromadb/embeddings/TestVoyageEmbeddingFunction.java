package tech.amikos.chromadb.embeddings;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.embeddings.voyage.VoyageEmbeddingFunction;
import tech.amikos.chromadb.v2.ChromaException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

/**
 * Unit tests for VoyageEmbeddingFunction using WireMock to stub the Voyage API.
 */
public class TestVoyageEmbeddingFunction {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private String wireMockUrl() {
        return "http://localhost:" + wireMock.port() + "/v1/embeddings";
    }

    private VoyageEmbeddingFunction createFunction() throws EFException {
        return new VoyageEmbeddingFunction(
                WithParam.apiKey("test-key"),
                WithParam.baseAPI(wireMockUrl())
        );
    }

    @Test
    public void testEmbedDocuments() throws EFException {
        stubFor(post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[{\"embedding\":[0.1,0.2,0.3],\"index\":0},{\"embedding\":[0.4,0.5,0.6],\"index\":1}],\"usage\":{\"total_tokens\":10}}")));

        VoyageEmbeddingFunction ef = createFunction();
        List<Embedding> result = ef.embedDocuments(Arrays.asList("doc1", "doc2"));

        assertEquals(2, result.size());
        assertEquals(3, result.get(0).getDimensions());
        assertEquals(0.1f, result.get(0).asArray()[0], 0.01f);
        assertEquals(0.2f, result.get(0).asArray()[1], 0.01f);
        assertEquals(0.3f, result.get(0).asArray()[2], 0.01f);
    }

    @Test
    public void testEmbedQuery() throws EFException {
        stubFor(post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[{\"embedding\":[0.7,0.8,0.9],\"index\":0}],\"usage\":{\"total_tokens\":5}}")));

        VoyageEmbeddingFunction ef = createFunction();
        Embedding result = ef.embedQuery("test query");

        assertNotNull(result);
        assertEquals(3, result.getDimensions());

        // Verify request body contains input_type: "query"
        verify(postRequestedFor(urlEqualTo("/v1/embeddings"))
                .withRequestBody(containing("\"input_type\":\"query\"")));
    }

    @Test
    public void testEmbedDocumentsInputType() throws EFException {
        stubFor(post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[{\"embedding\":[0.1,0.2,0.3],\"index\":0}],\"usage\":{\"total_tokens\":5}}")));

        VoyageEmbeddingFunction ef = createFunction();
        ef.embedDocuments(Arrays.asList("doc1"));

        // Verify request body contains input_type: "document"
        verify(postRequestedFor(urlEqualTo("/v1/embeddings"))
                .withRequestBody(containing("\"input_type\":\"document\"")));
    }

    @Test
    public void testAuthHeader() throws EFException {
        stubFor(post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[{\"embedding\":[0.1,0.2,0.3],\"index\":0}],\"usage\":{\"total_tokens\":5}}")));

        VoyageEmbeddingFunction ef = createFunction();
        ef.embedDocuments(Arrays.asList("doc1"));

        // Verify Authorization header
        verify(postRequestedFor(urlEqualTo("/v1/embeddings"))
                .withHeader("Authorization", equalTo("Bearer test-key")));
    }

    @Test
    public void testErrorResponse() throws EFException {
        stubFor(post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"unauthorized\"}")));

        VoyageEmbeddingFunction ef = new VoyageEmbeddingFunction(
                WithParam.apiKey("bad-key"),
                WithParam.baseAPI(wireMockUrl())
        );
        try {
            ef.embedDocuments(Arrays.asList("doc1"));
            fail("Expected ChromaException for 401 response");
        } catch (ChromaException e) {
            assertTrue(
                    "Expected message to mention Voyage, got: " + e.getMessage(),
                    e.getMessage().contains("Voyage")
            );
        }
    }

    @Test
    public void testApiKeyFromEnvThrowsWhenNotSet() {
        try {
            new VoyageEmbeddingFunction();
            fail("Expected EFException when VOYAGE_API_KEY is not set");
        } catch (EFException e) {
            assertTrue(
                    "Expected message to mention VOYAGE_API_KEY, got: " + e.getMessage(),
                    e.getMessage().contains("VOYAGE_API_KEY")
            );
        }
    }

    @Test
    public void testEmbedQueryRejectsNull() throws EFException {
        VoyageEmbeddingFunction ef = createFunction();

        try {
            ef.embedQuery(null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("query must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsRejectsNull() throws EFException {
        VoyageEmbeddingFunction ef = createFunction();

        try {
            ef.embedDocuments((List<String>) null);
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("documents must not be null"));
        }
    }

    @Test
    public void testEmbedDocumentsRejectsEmptyList() throws EFException {
        VoyageEmbeddingFunction ef = createFunction();

        try {
            ef.embedDocuments(Collections.<String>emptyList());
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("documents must not be empty"));
        }
    }

    @Test
    public void testMissingApiKeyFailsFast() throws EFException {
        VoyageEmbeddingFunction ef = new VoyageEmbeddingFunction(WithParam.baseAPI(wireMockUrl()));

        try {
            ef.embedDocuments(Arrays.asList("doc1"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("API key must not be null or empty"));
        }
    }

    @Test
    public void testEmptyResponseBodyFailsDescriptively() throws EFException {
        stubFor(post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        VoyageEmbeddingFunction ef = createFunction();

        try {
            ef.embedDocuments(Arrays.asList("doc1"));
            fail("Expected ChromaException");
        } catch (ChromaException e) {
            assertTrue(e.getMessage().contains("response body was empty"));
        }
    }
}
