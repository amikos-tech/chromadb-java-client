package tech.amikos.hf;

import com.google.gson.Gson;
import okhttp3.*;

import java.io.IOException;
import java.util.List;

//https://huggingface.co/blog/getting-started-with-embeddings
public class HuggingFaceClient {

    private String baseUrl = "https://api-inference.huggingface.co/pipeline/feature-extraction/";
    private String apiKey;

    private OkHttpClient client = new OkHttpClient();

    private String modelId = "sentence-transformers/all-MiniLM-L6-v2";
    private Gson gson = new Gson();
    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public HuggingFaceClient(String apiKey) {
        this.apiKey = apiKey;
    }


    public HuggingFaceClient apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }


    public HuggingFaceClient baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public HuggingFaceClient modelId(String modelId) {
        this.modelId = modelId;
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
                .url(this.baseUrl + this.modelId)
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

            List parsedResponse = gson.fromJson(responseData, List.class);

            return new CreateEmbeddingResponse(parsedResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
