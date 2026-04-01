package tech.amikos.chromadb.reranking.jina;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response body from the Jina v1 rerank API.
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
