package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.cohere.CohereEmbeddingFunction;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class CohereConformanceTest extends AbstractEmbeddingFunctionConformanceTest {

    private static final String MODEL = "embed-english-v2.0";
    // Cohere appends "embed" to the base URL
    private static final String BASE_PATH = "/v1/";
    private static final String ENDPOINT = "/v1/embed";

    @Override
    protected EmbeddingFunction createEmbeddingFunction(String baseUrl) throws EFException {
        return new CohereEmbeddingFunction(
                WithParam.baseAPI(baseUrl + BASE_PATH),
                WithParam.apiKey("test-key")
        );
    }

    @Override
    protected void stubSuccess(int inputCount) {
        StringBuilder embeddings = new StringBuilder();
        for (int i = 0; i < inputCount; i++) {
            if (i > 0) embeddings.append(",");
            embeddings.append("[0.1,0.2,0.3]");
        }
        stubFor(post(urlEqualTo(ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"embeddings\":[" + embeddings + "]}")));
    }

    @Override
    protected void stubFailure(int httpStatus, String body) {
        stubFor(post(urlEqualTo(ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(httpStatus)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    @Override
    protected void stubCountMismatch(int inputCount, int returnCount) {
        StringBuilder embeddings = new StringBuilder();
        for (int i = 0; i < returnCount; i++) {
            if (i > 0) embeddings.append(",");
            embeddings.append("[0.1,0.2,0.3]");
        }
        stubFor(post(urlEqualTo(ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"embeddings\":[" + embeddings + "]}")));
    }

    @Override
    protected String providerName() {
        return "Cohere";
    }

    @Override
    protected String modelName() {
        return MODEL;
    }
}
