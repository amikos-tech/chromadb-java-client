package tech.amikos.chromadb.reranking;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.reranking.cohere.CohereRerankingFunction;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

public class TestCohereRerankingFunction {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private String wireMockBaseUrl;

    @Before
    public void setUp() {
        wireMockBaseUrl = "http://localhost:" + wireMock.port() + "/v2/rerank";
    }

    @Test
    public void testRerankSuccess() throws EFException {
        stubFor(post(urlEqualTo("/v2/rerank"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"index\":1,\"relevance_score\":0.95},{\"index\":0,\"relevance_score\":0.72}]}")));

        CohereRerankingFunction reranker = new CohereRerankingFunction(
                WithParam.apiKey("test-key"), WithParam.baseAPI(wireMockBaseUrl));
        List<RerankResult> results = reranker.rerank("query", Arrays.asList("doc0", "doc1"));

        assertEquals(2, results.size());
        // Sorted by descending score
        assertEquals(1, results.get(0).getIndex());
        assertEquals(0.95, results.get(0).getScore(), 0.0001);
        assertEquals(0, results.get(1).getIndex());
        assertEquals(0.72, results.get(1).getScore(), 0.0001);
    }

    @Test
    public void testRerankAuthFailure() throws EFException {
        stubFor(post(urlEqualTo("/v2/rerank"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"invalid api key\"}")));

        CohereRerankingFunction reranker = new CohereRerankingFunction(
                WithParam.apiKey("bad-key"), WithParam.baseAPI(wireMockBaseUrl));
        try {
            reranker.rerank("query", Arrays.asList("doc0"));
            fail("Expected EFException");
        } catch (EFException e) {
            assertTrue(e.getMessage().contains("401"));
        }
    }

    @Test
    public void testRequestContainsAuthHeader() throws EFException {
        stubFor(post(urlEqualTo("/v2/rerank"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"index\":0,\"relevance_score\":0.5}]}")));

        CohereRerankingFunction reranker = new CohereRerankingFunction(
                WithParam.apiKey("test-key"), WithParam.baseAPI(wireMockBaseUrl));
        reranker.rerank("query", Arrays.asList("doc0"));

        verify(postRequestedFor(urlEqualTo("/v2/rerank"))
                .withHeader("Authorization", equalTo("Bearer test-key")));
    }

    @Test
    public void testMissingApiKeyFailsFast() throws EFException {
        CohereRerankingFunction reranker = new CohereRerankingFunction(WithParam.baseAPI(wireMockBaseUrl));

        try {
            reranker.rerank("query", Arrays.asList("doc0"));
            fail("Expected EFException");
        } catch (EFException e) {
            assertTrue(e.getMessage().contains("API key must not be null or empty"));
        }
    }

    @Test
    public void testNullQueryRejected() throws EFException {
        CohereRerankingFunction reranker = new CohereRerankingFunction(
                WithParam.apiKey("test-key"), WithParam.baseAPI(wireMockBaseUrl));

        try {
            reranker.rerank(null, Arrays.asList("doc0"));
            fail("Expected EFException");
        } catch (EFException e) {
            assertTrue(e.getMessage().contains("query must not be null"));
        }
    }

    @Test
    public void testNullDocumentsRejected() throws EFException {
        CohereRerankingFunction reranker = new CohereRerankingFunction(
                WithParam.apiKey("test-key"), WithParam.baseAPI(wireMockBaseUrl));

        try {
            reranker.rerank("query", null);
            fail("Expected EFException");
        } catch (EFException e) {
            assertTrue(e.getMessage().contains("documents must not be null"));
        }
    }

    @Test
    public void testMissingResultsRejected() throws EFException {
        stubFor(post(urlEqualTo("/v2/rerank"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        CohereRerankingFunction reranker = new CohereRerankingFunction(
                WithParam.apiKey("test-key"), WithParam.baseAPI(wireMockBaseUrl));

        try {
            reranker.rerank("query", Arrays.asList("doc0"));
            fail("Expected EFException");
        } catch (EFException e) {
            assertTrue(e.getMessage().contains("response did not contain results"));
        }
    }
}
