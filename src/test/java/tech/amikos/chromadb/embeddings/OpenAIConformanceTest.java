package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class OpenAIConformanceTest extends AbstractEmbeddingFunctionConformanceTest {

    private static final String MODEL = "text-embedding-ada-002";
    private static final String ENDPOINT = "/v1/embeddings";

    @Override
    protected EmbeddingFunction createEmbeddingFunction(String baseUrl) throws EFException {
        return new OpenAIEmbeddingFunction(
                WithParam.baseAPI(baseUrl + ENDPOINT),
                WithParam.apiKey("test-key")
        );
    }

    @Override
    protected void stubSuccess(int inputCount) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < inputCount; i++) {
            if (i > 0) data.append(",");
            data.append("{\"embedding\":[0.1,0.2,0.3],\"index\":").append(i).append("}");
        }
        stubFor(post(urlEqualTo(ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[" + data + "],\"model\":\"" + MODEL + "\"}")));
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
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < returnCount; i++) {
            if (i > 0) data.append(",");
            data.append("{\"embedding\":[0.1,0.2,0.3],\"index\":").append(i).append("}");
        }
        stubFor(post(urlEqualTo(ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[" + data + "],\"model\":\"" + MODEL + "\"}")));
    }

    @Override
    protected String providerName() {
        return "OpenAI";
    }

    @Override
    protected String modelName() {
        return MODEL;
    }
}
