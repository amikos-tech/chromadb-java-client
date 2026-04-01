package tech.amikos.chromadb.embeddings.voyage;

import com.google.gson.annotations.SerializedName;
import tech.amikos.chromadb.Embedding;

import java.util.ArrayList;
import java.util.List;

/**
 * Response body from the Voyage AI embeddings API.
 */
public class CreateEmbeddingResponse {

    @SerializedName("data")
    private List<DataItem> data;

    @SerializedName("usage")
    private Usage usage;

    public List<DataItem> getData() {
        return data;
    }

    /**
     * Converts the response data items to a list of Embedding objects.
     *
     * @return list of embeddings in order of their index
     */
    public List<Embedding> toEmbeddings() {
        List<Embedding> embeddings = new ArrayList<Embedding>();
        if (data != null) {
            for (DataItem item : data) {
                float[] floatArray = new float[item.embedding.size()];
                for (int i = 0; i < item.embedding.size(); i++) {
                    floatArray[i] = item.embedding.get(i);
                }
                embeddings.add(new Embedding(floatArray));
            }
        }
        return embeddings;
    }

    public static class DataItem {
        @SerializedName("embedding")
        List<Float> embedding;

        @SerializedName("index")
        int index;

        public List<Float> getEmbedding() {
            return embedding;
        }

        public int getIndex() {
            return index;
        }
    }

    static class Usage {
        @SerializedName("total_tokens")
        int totalTokens;
    }
}
