package tech.amikos.openai;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;

public class OpenAIClient {

    private String baseUrl = "https://api.openai.com/v1/";
    private String apiKey;

    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public OpenAIClient() {

    }


    public OpenAIClient apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public OpenAIClient baseUrl(String baseUrl) {
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
                .url(this.baseUrl + "embeddings")
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
            e.printStackTrace();
        }
        return null;
    }


}

