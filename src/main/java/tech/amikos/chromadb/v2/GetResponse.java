package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class GetResponse {
    @SerializedName("ids")
    private List<String> ids;

    @SerializedName("embeddings")
    private List<List<Float>> embeddings;

    @SerializedName("documents")
    private List<String> documents;

    @SerializedName("metadatas")
    private List<Map<String, Object>> metadatas;

    @SerializedName("uris")
    private List<String> uris;

    @SerializedName("include")
    private List<Include> include;

    public List<String> getIds() {
        return ids;
    }

    public List<List<Float>> getEmbeddings() {
        return embeddings;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public List<Map<String, Object>> getMetadatas() {
        return metadatas;
    }

    public List<String> getUris() {
        return uris;
    }

    public List<Include> getInclude() {
        return include;
    }
}