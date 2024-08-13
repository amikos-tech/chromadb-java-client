package tech.amikos.chromadb.embeddings.cohere;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;

public class CohereClient {

    private String baseUrl = "https://api.cohere.ai/";

    private String apiVersion = "v1";
    private String apiKey;

    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public CohereClient(String apiKey) {
        this.apiKey = apiKey;
    }


    public CohereClient apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public CohereClient apiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    public CohereClient baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    private String getApiKey() {
        if (this.apiKey == null) {
            throw new RuntimeException("API Key not set");
        }
        return this.apiKey;
    }

    public CreateEmbeddingResponse createEmbedding(CreateEmbeddingRequest req) {
        Request request = new Request.Builder()
                .url(this.baseUrl + apiVersion + "/embed")
                .post(RequestBody.create(req.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Request-Source", "chroma-java-client")
                .addHeader("Authorization", "Bearer " + getApiKey())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseData = response.body().string();

            return gson.fromJson(responseData, CreateEmbeddingResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
