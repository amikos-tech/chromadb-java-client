package tech.amikos.chromadb;

import java.util.List;

public interface EmbeddingFunction {

    List<List<Float>> createEmbedding(List<String> documents);
}
