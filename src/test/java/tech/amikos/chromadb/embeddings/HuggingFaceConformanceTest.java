package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class HuggingFaceConformanceTest extends AbstractEmbeddingFunctionConformanceTest {

    private static final String MODEL = "test-model";
    private static final String MODEL_PATH = "/" + MODEL;

    @Override
    protected EmbeddingFunction createEmbeddingFunction(String baseUrl) throws EFException {
        return new HuggingFaceEmbeddingFunction(
                WithParam.baseAPI(baseUrl + "/"),
                WithParam.apiKey("test-key"),
                WithParam.model(MODEL)
        );
    }

    @Override
    protected void stubSuccess(int inputCount) {
        StringBuilder embeddings = new StringBuilder();
        for (int i = 0; i < inputCount; i++) {
            if (i > 0) embeddings.append(",");
            embeddings.append("[0.1,0.2,0.3]");
        }
        stubFor(post(urlPathMatching(MODEL_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[" + embeddings + "]")));
    }

    @Override
    protected void stubFailure(int httpStatus, String body) {
        stubFor(post(urlPathMatching(MODEL_PATH))
                .willReturn(aResponse()
                        .withStatus(httpStatus)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }

    @Override
    protected void stubCountMismatch(int inputCount, int returnCount) {
        // Return returnCount embeddings regardless of inputCount
        StringBuilder embeddings = new StringBuilder();
        for (int i = 0; i < returnCount; i++) {
            if (i > 0) embeddings.append(",");
            embeddings.append("[0.1,0.2,0.3]");
        }
        stubFor(post(urlPathMatching(MODEL_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[" + embeddings + "]")));
    }

    @Override
    protected String providerName() {
        return "HuggingFace";
    }

    @Override
    protected String modelName() {
        return MODEL;
    }
}
