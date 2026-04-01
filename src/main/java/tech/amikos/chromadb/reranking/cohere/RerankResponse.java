package tech.amikos.chromadb.reranking.cohere;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response body from the Cohere v2 rerank API.
 */
class RerankResponse {

    @SerializedName("results")
    List<Result> results;

    static class Result {
        @SerializedName("index")
        int index;

        @SerializedName("relevance_score")
        double relevance_score;
    }
}
