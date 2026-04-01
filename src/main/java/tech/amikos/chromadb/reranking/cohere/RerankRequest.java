package tech.amikos.chromadb.reranking.cohere;

import com.google.gson.Gson;

import java.util.List;

/**
 * Request body for the Cohere v2 rerank API.
 */
class RerankRequest {

    private final String model;
    private final String query;
    private final List<String> documents;

    RerankRequest(String model, String query, List<String> documents) {
        this.model = model;
        this.query = query;
        this.documents = documents;
    }

    String json() {
        return new Gson().toJson(this);
    }
}
