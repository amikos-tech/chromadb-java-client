package tech.amikos.chromadb.reranking;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.reranking.jina.JinaRerankingFunction;

import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

public class TestJinaRerankingFunction {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private String wireMockBaseUrl;

    @Before
    public void setUp() {
        wireMockBaseUrl = "http://localhost:" + wireMock.port() + "/v1/rerank";
    }

    @Test
    public void testRerankSuccess() throws EFException {
        stubFor(post(urlEqualTo("/v1/rerank"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"index\":0,\"relevance_score\":0.9},{\"index\":1,\"relevance_score\":0.3}]}")));

        JinaRerankingFunction reranker = new JinaRerankingFunction(
                WithParam.apiKey("test-key"), WithParam.baseAPI(wireMockBaseUrl));
        List<RerankResult> results = reranker.rerank("query", Arrays.asList("doc0", "doc1"));

        assertEquals(2, results.size());
        assertEquals(0, results.get(0).getIndex());
        assertEquals(0.9, results.get(0).getScore(), 0.0001);
        assertEquals(1, results.get(1).getIndex());
        assertEquals(0.3, results.get(1).getScore(), 0.0001);
    }

    @Test
    public void testRerankServerError() throws EFException {
        stubFor(post(urlEqualTo("/v1/rerank"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"internal server error\"}")));

        JinaRerankingFunction reranker = new JinaRerankingFunction(
                WithParam.apiKey("test-key"), WithParam.baseAPI(wireMockBaseUrl));
        try {
            reranker.rerank("query", Arrays.asList("doc0"));
            fail("Expected EFException");
        } catch (EFException e) {
            assertTrue(e.getMessage().contains("500"));
        }
    }

    @Test
    public void testRequestContainsModel() throws EFException {
        stubFor(post(urlEqualTo("/v1/rerank"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"index\":0,\"relevance_score\":0.5}]}")));

        JinaRerankingFunction reranker = new JinaRerankingFunction(
                WithParam.apiKey("test-key"), WithParam.baseAPI(wireMockBaseUrl));
        reranker.rerank("query", Arrays.asList("doc0"));

        verify(postRequestedFor(urlEqualTo("/v1/rerank"))
                .withRequestBody(containing("\"model\":\"jina-reranker-v2-base-multilingual\"")));
    }
}
