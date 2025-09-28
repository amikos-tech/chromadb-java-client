package tech.amikos.chromadb.v2.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class QueryResponse {
    @SerializedName("ids")
    private List<List<String>> ids;

    @SerializedName("embeddings")
    private List<List<List<Float>>> embeddings;

    @SerializedName("documents")
    private List<List<String>> documents;

    @SerializedName("metadatas")
    private List<List<Map<String, Object>>> metadatas;

    @SerializedName("distances")
    private List<List<Float>> distances;

    @SerializedName("uris")
    private List<List<String>> uris;

    public List<List<String>> getIds() {
        return ids;
    }

    public List<List<List<Float>>> getEmbeddings() {
        return embeddings;
    }

    public List<List<String>> getDocuments() {
        return documents;
    }

    public List<List<Map<String, Object>>> getMetadatas() {
        return metadatas;
    }

    public List<List<Float>> getDistances() {
        return distances;
    }

    public List<List<String>> getUris() {
        return uris;
    }
}