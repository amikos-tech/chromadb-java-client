package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class OllamaConformanceTest extends AbstractEmbeddingFunctionConformanceTest {

    private static final String MODEL = "nomic-embed-text";
    private static final String ENDPOINT = "/api/embed";

    @Override
    protected EmbeddingFunction createEmbeddingFunction(String baseUrl) throws EFException {
        return new OllamaEmbeddingFunction(
                WithParam.baseAPI(baseUrl + ENDPOINT)
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
        return "Ollama";
    }

    @Override
    protected String modelName() {
        return MODEL;
    }
}
