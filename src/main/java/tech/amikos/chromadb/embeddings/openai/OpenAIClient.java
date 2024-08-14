package tech.amikos.chromadb.embeddings.openai;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;

public class OpenAIClient {

    private String baseUrl = "https://api.openai.com/v1/embeddings";
    private String apiKey;

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public OpenAIClient() {

    }


    public OpenAIClient apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public OpenAIClient baseUrl(String baseUrl) {
        if (baseUrl == null){ //early exit if null (we default)
            return this;
        }
        this.baseUrl = baseUrl;
        return this;
    }

    private String getApiKey(){
        if (this.apiKey == null) {
            throw new RuntimeException("API Key not set");
        }
        return this.apiKey;
    }

    public CreateEmbeddingResponse createEmbedding(CreateEmbeddingRequest req) {
        Request request = new Request.Builder()
                .url(this.baseUrl)
                .post(RequestBody.create(req.json(), JSON))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + getApiKey())
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseData = response.body().string();

            return gson.fromJson(responseData, CreateEmbeddingResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}

